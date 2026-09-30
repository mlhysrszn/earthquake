package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PersistedEarthquakeSnapshot(
    val earthquakes: List<EarthquakeEntity>,
    val syncMetadata: SyncMetadataEntity?,
)

@Dao
abstract class EarthquakeDao {
    @Query("SELECT * FROM earthquakes ORDER BY occurredAtEpochMillis DESC")
    abstract fun observeAllEarthquakes(): Flow<List<EarthquakeEntity>>

    /**
     * Emits the rolling 24-hour window, re-reading [currentTimeMillis] on every table change.
     * A window fixed at subscription time would hide events that occur after the screen opened.
     */
    fun observeRecent(currentTimeMillis: () -> Long): Flow<List<EarthquakeEntity>> =
        observeAllEarthquakes().map { entities ->
            val nowEpochMillis = currentTimeMillis()
            val windowStartEpochMillis = nowEpochMillis - ROLLING_WINDOW_MILLIS
            entities.filter { entity ->
                entity.occurredAtEpochMillis in windowStartEpochMillis..nowEpochMillis
            }
        }

    @Query("SELECT * FROM earthquakes WHERE id = :id")
    abstract suspend fun findEarthquake(id: String): EarthquakeEntity?

    @Query("SELECT * FROM earthquakes WHERE id = :id")
    abstract fun observeEarthquake(id: String): Flow<EarthquakeEntity?>

    @Query("SELECT * FROM earthquakes ORDER BY occurredAtEpochMillis DESC")
    abstract suspend fun getAllEarthquakes(): List<EarthquakeEntity>

    @Upsert
    abstract suspend fun upsertEarthquakes(earthquakes: List<EarthquakeEntity>)

    @Transaction
    open suspend fun upsertDetailIfRecent(
        earthquake: EarthquakeEntity,
        nowEpochMillis: Long,
    ): Boolean {
        val windowStartEpochMillis = nowEpochMillis - ROLLING_WINDOW_MILLIS
        if (earthquake.occurredAtEpochMillis !in windowStartEpochMillis..nowEpochMillis) {
            return false
        }
        upsertEarthquakes(listOf(earthquake))
        return true
    }

    @Query("DELETE FROM earthquakes")
    abstract suspend fun deleteAllEarthquakes()

    @Query("SELECT * FROM sync_metadata WHERE id = ${SyncMetadataEntity.SINGLETON_ID}")
    abstract suspend fun getSyncMetadata(): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE id = ${SyncMetadataEntity.SINGLETON_ID}")
    abstract fun observeSyncMetadata(): Flow<SyncMetadataEntity?>

    @Transaction
    open suspend fun loadPersistedSnapshot(): PersistedEarthquakeSnapshot =
        PersistedEarthquakeSnapshot(
            earthquakes = getAllEarthquakes(),
            syncMetadata = getSyncMetadata(),
        )

    @Upsert
    abstract suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)

    /** Applies a complete rolling-window snapshot atomically, unless its source is stale. */
    @Transaction
    open suspend fun applySnapshot(
        earthquakes: List<EarthquakeEntity>,
        sourceGeneratedAtEpochMillis: Long?,
        fetchedAtEpochMillis: Long,
        nowEpochMillis: Long,
    ): SnapshotWriteResult {
        val previousMetadata = getSyncMetadata()
        val previousSourceTime = previousMetadata?.sourceGeneratedAtEpochMillis
        val isStale = previousSourceTime != null &&
            (sourceGeneratedAtEpochMillis == null ||
                sourceGeneratedAtEpochMillis < previousSourceTime)

        if (isStale) {
            upsertSyncMetadata(
                checkNotNull(previousMetadata).copy(
                    lastSuccessfulFetchAtEpochMillis = fetchedAtEpochMillis,
                ),
            )
            return SnapshotWriteResult.IgnoredStale(
                latestSourceGeneratedAtEpochMillis = checkNotNull(previousSourceTime),
            )
        }

        val windowStartEpochMillis = nowEpochMillis - ROLLING_WINDOW_MILLIS
        val windowEarthquakes = earthquakes.filter { earthquake ->
            earthquake.occurredAtEpochMillis in windowStartEpochMillis..nowEpochMillis
        }
        deleteAllEarthquakes()
        if (windowEarthquakes.isNotEmpty()) {
            upsertEarthquakes(windowEarthquakes)
        }
        upsertSyncMetadata(
            SyncMetadataEntity(
                sourceGeneratedAtEpochMillis =
                    sourceGeneratedAtEpochMillis ?: previousSourceTime,
                lastSuccessfulFetchAtEpochMillis = fetchedAtEpochMillis,
            ),
        )
        return SnapshotWriteResult.Applied(storedEventCount = windowEarthquakes.size)
    }

    private companion object {
        const val ROLLING_WINDOW_MILLIS = 24L * 60 * 60 * 1_000
    }
}
