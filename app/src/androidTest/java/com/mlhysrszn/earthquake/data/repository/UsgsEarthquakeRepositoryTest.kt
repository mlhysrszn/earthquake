package com.mlhysrszn.earthquake.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase
import com.mlhysrszn.earthquake.data.local.room.EarthquakeEntity
import com.mlhysrszn.earthquake.data.local.room.SyncMetadataEntity
import com.mlhysrszn.earthquake.data.remote.usgs.UsgsEarthquakeService
import com.mlhysrszn.earthquake.data.remote.usgs.UsgsGeoJsonMapper
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsgsEarthquakeRepositoryTest {
    private val nowEpochMillis = 1_790_597_000_000L
    private val sourceGeneratedAtEpochMillis = nowEpochMillis - 120_000
    private val fixedClock = Clock.fixed(
        Instant.ofEpochMilli(nowEpochMillis),
        ZoneOffset.UTC,
    )
    private val jsonMapper = UsgsGeoJsonMapper(Json { ignoreUnknownKeys = true })

    @Test
    fun freshFeedIsFetchedMappedAndAtomicallyStored() = runBlocking {
        val service = FakeUsgsEarthquakeService { responseBody(validFeed()) }
        withRepository(service) { dao, repository ->
            assertEquals(RefreshResult.Success(acceptedEventCount = 1), repository.refresh())

            val events = repository.observeEarthquakes().first()
            assertEquals(listOf("live-event-1"), events.map { it.id })
            assertEquals(-0.4, events.single().magnitude!!, 0.0)
            assertEquals(-149.0, events.single().longitude!!, 0.0)
            assertEquals(61.0, events.single().latitude!!, 0.0)
            assertEquals(
                SyncMetadataEntity(
                    sourceGeneratedAtEpochMillis = sourceGeneratedAtEpochMillis,
                    lastSuccessfulFetchAtEpochMillis = nowEpochMillis,
                ),
                dao.getSyncMetadata(),
            )
        }
    }

    @Test
    fun networkFailureReturnsTypedErrorAndPreservesCachedEvents() = runBlocking {
        val service = FakeUsgsEarthquakeService { throw IOException("offline") }
        withRepository(service) { dao, repository ->
            seedCachedEvent(dao)

            assertEquals(
                RefreshResult.Failure(RefreshResult.Reason.NETWORK),
                repository.refresh(),
            )
            assertEquals(listOf("cached-event"), repository.observeEarthquakes().first().map { it.id })
            assertEquals(sourceGeneratedAtEpochMillis, dao.getSyncMetadata()?.sourceGeneratedAtEpochMillis)
            assertEquals(nowEpochMillis - 1_000, dao.getSyncMetadata()?.lastSuccessfulFetchAtEpochMillis)
        }
    }

    @Test
    fun malformedFeedReturnsInvalidResponseAndPreservesCachedEvents() = runBlocking {
        val service = FakeUsgsEarthquakeService { responseBody("{") }
        withRepository(service) { dao, repository ->
            seedCachedEvent(dao)

            assertEquals(
                RefreshResult.Failure(RefreshResult.Reason.INVALID_RESPONSE),
                repository.refresh(),
            )
            assertEquals(listOf("cached-event"), repository.observeEarthquakes().first().map { it.id })
            assertEquals(nowEpochMillis - 1_000, dao.getSyncMetadata()?.lastSuccessfulFetchAtEpochMillis)
        }
    }

    @Test
    fun staleFeedIsNotAppliedEvenThoughTheFetchSucceeded() = runBlocking {
        val staleGeneratedAt = sourceGeneratedAtEpochMillis - 1
        val service = FakeUsgsEarthquakeService {
            responseBody(validFeed(generatedAt = staleGeneratedAt, id = "stale-event"))
        }
        withRepository(service) { dao, repository ->
            seedCachedEvent(dao)

            assertEquals(RefreshResult.Success(acceptedEventCount = 1), repository.refresh())
            assertEquals(listOf("cached-event"), repository.observeEarthquakes().first().map { it.id })
            assertEquals(sourceGeneratedAtEpochMillis, dao.getSyncMetadata()?.sourceGeneratedAtEpochMillis)
            assertEquals(nowEpochMillis, dao.getSyncMetadata()?.lastSuccessfulFetchAtEpochMillis)
        }
    }

    @Test
    fun overlappingRefreshesAreSerialized() = runBlocking {
        val firstRequestStarted = CompletableDeferred<Unit>()
        val allowFirstRequestToFinish = CompletableDeferred<Unit>()
        val service = FakeUsgsEarthquakeService { callNumber ->
            if (callNumber == 1) {
                firstRequestStarted.complete(Unit)
                allowFirstRequestToFinish.await()
            }
            responseBody(validFeed())
        }
        withRepository(service) { _, repository ->
            val firstRefresh = async { repository.refresh() }
            firstRequestStarted.await()
            val secondRefresh = async { repository.refresh() }
            yield()

            assertEquals(1, service.callCount)
            assertEquals(1, service.maximumConcurrentCalls)

            allowFirstRequestToFinish.complete(Unit)
            assertEquals(RefreshResult.Success(1), firstRefresh.await())
            assertEquals(RefreshResult.Success(1), secondRefresh.await())
            assertEquals(2, service.callCount)
            assertEquals(1, service.maximumConcurrentCalls)
        }
    }

    @Test
    fun cancellingAnInFlightRefreshPropagatesCancellation() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val pendingBody = CompletableDeferred<ResponseBody>()
        val service = FakeUsgsEarthquakeService {
            requestStarted.complete(Unit)
            pendingBody.await()
        }
        withRepository(service) { dao, repository ->
            val refreshJob = launch { repository.refresh() }
            requestStarted.await()

            refreshJob.cancelAndJoin()

            assertTrue(refreshJob.isCancelled)
            assertNull(dao.getSyncMetadata())
        }
    }

    private suspend fun withRepository(
        service: UsgsEarthquakeService,
        test: suspend (EarthquakeDao, EarthquakeRepository) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            EarthquakeDatabase::class.java,
        ).build()
        try {
            val dao = database.earthquakeDao()
            val repository = UsgsEarthquakeRepository(
                earthquakeDao = dao,
                usgsService = service,
                mapper = jsonMapper,
                clock = fixedClock,
            )
            test(dao, repository)
        } finally {
            database.close()
        }
    }

    private suspend fun seedCachedEvent(dao: EarthquakeDao) {
        dao.applySnapshot(
            earthquakes = listOf(
                EarthquakeEntity(
                    id = "cached-event",
                    magnitude = 3.2,
                    magnitudeType = "ml",
                    place = "Cached location",
                    occurredAtEpochMillis = nowEpochMillis - 10_000,
                    updatedAtEpochMillis = null,
                    longitude = 12.0,
                    latitude = 34.0,
                    depthKm = 5.0,
                    sourceUrl = null,
                ),
            ),
            sourceGeneratedAtEpochMillis = sourceGeneratedAtEpochMillis,
            fetchedAtEpochMillis = nowEpochMillis - 1_000,
            nowEpochMillis = nowEpochMillis,
        )
    }

    private fun validFeed(
        generatedAt: Long = sourceGeneratedAtEpochMillis,
        id: String = "live-event-1",
    ): String = """
        {
          "type": "FeatureCollection",
          "metadata": {"generated": $generatedAt},
          "features": [{
            "type": "Feature",
            "id": "$id",
            "properties": {
              "mag": -0.4,
              "place": "Southeast Alaska",
              "time": ${nowEpochMillis - 60_000},
              "updated": ${nowEpochMillis - 30_000},
              "magType": "md",
              "type": "earthquake"
            },
            "geometry": {"type": "Point", "coordinates": [-149.0, 61.0, 20.0]}
          }]
        }
    """.trimIndent()

    private fun responseBody(payload: String): ResponseBody =
        payload.toResponseBody("application/json".toMediaType())

    private class FakeUsgsEarthquakeService(
        private val response: suspend (callNumber: Int) -> ResponseBody,
    ) : UsgsEarthquakeService {
        var callCount: Int = 0
            private set
        var activeCalls: Int = 0
            private set
        var maximumConcurrentCalls: Int = 0
            private set

        override suspend fun fetchAllDaySummary(): ResponseBody {
            val callNumber = ++callCount
            activeCalls++
            maximumConcurrentCalls = maxOf(maximumConcurrentCalls, activeCalls)
            return try {
                response(callNumber)
            } finally {
                activeCalls--
            }
        }
    }
}
