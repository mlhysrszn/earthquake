package com.mlhysrszn.earthquake.data.repository

import android.database.sqlite.SQLiteException
import com.mlhysrszn.earthquake.data.local.demo.DemoScenarioDao
import com.mlhysrszn.earthquake.data.local.demo.DemoScenarioEntity
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.SyncMetadataEntity
import com.mlhysrszn.earthquake.data.local.room.toDomain
import com.mlhysrszn.earthquake.data.local.room.toEntity
import com.mlhysrszn.earthquake.domain.model.DemoScenarioKind
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persists demo scenarios in the demo-only Room file but shares the live repository contract. */
@Singleton
class DemoEarthquakeRepository @Inject constructor(
    private val earthquakeDao: EarthquakeDao,
    private val demoScenarioDao: DemoScenarioDao,
    private val clock: Clock,
) : EarthquakeRepository {
    private val refreshMutex = Mutex()

    override fun observeEarthquakes(): Flow<List<Earthquake>> =
        earthquakeDao.observeRecent(clock::millis).map { entities -> entities.map { it.toDomain() } }

    override fun observeLastSuccessfulRefresh(): Flow<Instant?> =
        earthquakeDao.observeSyncMetadata().map { metadata ->
            metadata?.lastSuccessfulFetchAtEpochMillis?.let(Instant::ofEpochMilli)
        }

    override fun observeEarthquake(id: String): Flow<Earthquake?> =
        earthquakeDao.observeEarthquake(id).map { it?.toDomain() }

    override suspend fun refresh(): RefreshResult = try {
        refreshMutex.withLock {
            val now = clock.instant()
            val nowMillis = now.toEpochMilli()
            val previousCursor = earthquakeDao.getSyncMetadata()?.sourceGeneratedAtEpochMillis
            val generatedAtMillis = maxOf(
                nowMillis,
                previousCursor?.let { cursor -> if (cursor == Long.MAX_VALUE) cursor else cursor + 1 }
                    ?: nowMillis,
            )
            val events = demoScenarioDao.getAll().map { entry ->
                entry.toEarthquake()
            }
            val result = earthquakeDao.applySnapshot(
                earthquakes = events.map(Earthquake::toEntity),
                sourceGeneratedAtEpochMillis = generatedAtMillis,
                fetchedAtEpochMillis = nowMillis,
                nowEpochMillis = nowMillis,
            )
            when (result) {
                is com.mlhysrszn.earthquake.data.local.room.SnapshotWriteResult.Applied ->
                    RefreshResult.Success(acceptedEventCount = result.storedEventCount)

                is com.mlhysrszn.earthquake.data.local.room.SnapshotWriteResult.IgnoredStale ->
                    RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
            }
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: SQLiteException) {
        RefreshResult.Failure(RefreshResult.Reason.STORAGE)
    } catch (_: Exception) {
        RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
    }

    override suspend fun fetchEarthquakeById(id: String): EarthquakeLookupResult = try {
        earthquakeDao.findEarthquake(id)?.let { entity ->
            EarthquakeLookupResult.Found(entity.toDomain())
        } ?: EarthquakeLookupResult.Unavailable
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: SQLiteException) {
        EarthquakeLookupResult.Failure(RefreshResult.Reason.STORAGE)
    } catch (_: Exception) {
        EarthquakeLookupResult.Failure(RefreshResult.Reason.UNKNOWN)
    }

    private fun DemoScenarioEntity.toEarthquake(): Earthquake {
        val eventTime = Instant.ofEpochMilli(addedAtEpochMillis)
        val details = when (DemoScenarioKind.valueOf(kind)) {
            DemoScenarioKind.BELOW_THRESHOLD -> DemoEventDetails(
                magnitude = 3.5,
                place = "DEMO · Below threshold",
            )

            DemoScenarioKind.ABOVE_THRESHOLD -> DemoEventDetails(
                magnitude = 5.5,
                place = "DEMO · Above threshold",
            )
        }
        return Earthquake(
            id = id,
            magnitude = details.magnitude,
            magnitudeType = "demo",
            place = details.place,
            occurredAt = eventTime,
            updatedAt = eventTime,
            longitude = null,
            latitude = null,
            depthKm = null,
            sourceUrl = null,
        )
    }

    private data class DemoEventDetails(
        val magnitude: Double,
        val place: String,
    )
}
