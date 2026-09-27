package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakeDatabaseTest {
    private val nowEpochMillis = 24L * 60 * 60 * 1_000 + 15_000L

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun upsertInsertsAndUpdatesByEventIdentity() = runBlocking {
        withInMemoryDao { dao ->
            dao.upsertEarthquakes(listOf(event(id = "event-1", time = 8_000, place = "Original")))
            dao.upsertEarthquakes(listOf(event(id = "event-1", time = 9_000, place = "Revised")))

            val stored = dao.getAllEarthquakes()

            assertEquals(1, stored.size)
            assertEquals("Revised", stored.single().place)
            assertEquals(9_000L, stored.single().occurredAtEpochMillis)
        }
    }

    @Test
    fun snapshotTransactionReplacesRowsAndMetadataAndEnforcesWindow() = runBlocking {
        withInMemoryDao { dao ->
            dao.upsertEarthquakes(listOf(event(id = "old", time = 7_000)))

            val result = dao.applySnapshot(
                earthquakes = listOf(
                    event(id = "at-cutoff", time = 15_000),
                    event(id = "recent", time = nowEpochMillis),
                    event(id = "too-old", time = 14_999),
                    event(id = "future", time = nowEpochMillis + 1),
                ),
                sourceGeneratedAtEpochMillis = 20_000,
                fetchedAtEpochMillis = 21_000,
                nowEpochMillis = nowEpochMillis,
            )

            assertEquals(SnapshotWriteResult.Applied(storedEventCount = 2), result)
            assertEquals(
                listOf("recent", "at-cutoff"),
                dao.observeRecent(nowEpochMillis)
                    .first()
                    .map(EarthquakeEntity::id),
            )
            assertEquals(
                listOf("recent", "at-cutoff"),
                dao.getAllEarthquakes().map(EarthquakeEntity::id),
            )
            assertEquals(
                SyncMetadataEntity(
                    sourceGeneratedAtEpochMillis = 20_000,
                    lastSuccessfulFetchAtEpochMillis = 21_000,
                ),
                dao.getSyncMetadata(),
            )
        }
    }

    @Test
    fun olderAndUnversionedSnapshotsCannotReplaceNewerSourceData() = runBlocking {
        withInMemoryDao { dao ->
            dao.applySnapshot(
                earthquakes = listOf(
                    event(id = "current", time = nowEpochMillis - 1_000, place = "Current"),
                ),
                sourceGeneratedAtEpochMillis = 20_000,
                fetchedAtEpochMillis = 20_100,
                nowEpochMillis = nowEpochMillis,
            )

            val staleResult = dao.applySnapshot(
                earthquakes = listOf(event(id = "stale", time = nowEpochMillis - 500)),
                sourceGeneratedAtEpochMillis = 19_999,
                fetchedAtEpochMillis = 20_200,
                nowEpochMillis = nowEpochMillis,
            )
            val unversionedResult = dao.applySnapshot(
                earthquakes = listOf(event(id = "unversioned", time = nowEpochMillis)),
                sourceGeneratedAtEpochMillis = null,
                fetchedAtEpochMillis = 20_300,
                nowEpochMillis = nowEpochMillis,
            )

            assertEquals(SnapshotWriteResult.IgnoredStale(20_000), staleResult)
            assertEquals(SnapshotWriteResult.IgnoredStale(20_000), unversionedResult)
            assertEquals(listOf("current"), dao.getAllEarthquakes().map(EarthquakeEntity::id))
            assertEquals(
                20_000L,
                dao.getSyncMetadata()?.sourceGeneratedAtEpochMillis,
            )
            assertEquals(
                20_300L,
                dao.getSyncMetadata()?.lastSuccessfulFetchAtEpochMillis,
            )
        }
    }

    @Test
    fun initialUnversionedSnapshotCanBeAppliedAndEmptyFreshFeedClearsRows() = runBlocking {
        withInMemoryDao { dao ->
            val initialResult = dao.applySnapshot(
                earthquakes = listOf(event(id = "initial", time = nowEpochMillis - 1_000)),
                sourceGeneratedAtEpochMillis = null,
                fetchedAtEpochMillis = 10_000,
                nowEpochMillis = nowEpochMillis,
            )
            val emptyResult = dao.applySnapshot(
                earthquakes = emptyList(),
                sourceGeneratedAtEpochMillis = 20_000,
                fetchedAtEpochMillis = 21_000,
                nowEpochMillis = nowEpochMillis,
            )

            assertEquals(SnapshotWriteResult.Applied(1), initialResult)
            assertEquals(SnapshotWriteResult.Applied(0), emptyResult)
            assertEquals(emptyList<EarthquakeEntity>(), dao.getAllEarthquakes())
            assertEquals(20_000L, dao.getSyncMetadata()?.sourceGeneratedAtEpochMillis)
        }
    }

    @Test
    fun snapshotAndSyncMetadataPersistAfterReopeningDatabase() = runBlocking {
        val databaseName = "earthquake-test-${UUID.randomUUID()}.db"
        val firstDatabase = Room.databaseBuilder(
            context,
            EarthquakeDatabase::class.java,
            databaseName,
        ).build()
        try {
            firstDatabase.earthquakeDao().applySnapshot(
                earthquakes = listOf(event(id = "persisted", time = nowEpochMillis - 1_000)),
                sourceGeneratedAtEpochMillis = 20_000,
                fetchedAtEpochMillis = 20_100,
                nowEpochMillis = nowEpochMillis,
            )
        } finally {
            firstDatabase.close()
        }

        val reopenedDatabase = Room.databaseBuilder(
            context,
            EarthquakeDatabase::class.java,
            databaseName,
        ).build()
        try {
            val dao = reopenedDatabase.earthquakeDao()
            assertEquals("persisted", dao.findEarthquake("persisted")?.id)
            assertEquals(20_000L, dao.getSyncMetadata()?.sourceGeneratedAtEpochMillis)
        } finally {
            reopenedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private suspend fun withInMemoryDao(test: suspend (EarthquakeDao) -> Unit) {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            EarthquakeDatabase::class.java,
        ).build()
        try {
            test(database.earthquakeDao())
        } finally {
            database.close()
        }
    }

    private fun event(
        id: String,
        time: Long,
        place: String = id,
    ) = EarthquakeEntity(
        id = id,
        magnitude = 4.5,
        magnitudeType = "ml",
        place = place,
        occurredAtEpochMillis = time,
        updatedAtEpochMillis = null,
        longitude = -122.0,
        latitude = 37.0,
        depthKm = 8.0,
        sourceUrl = null,
    )
}
