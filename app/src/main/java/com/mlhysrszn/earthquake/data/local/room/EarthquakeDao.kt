package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EarthquakeDao {
    @Query(
        """
        SELECT * FROM earthquakes
        WHERE occurredAtEpochMillis >= :windowStartEpochMillis
          AND occurredAtEpochMillis <= :nowEpochMillis
        ORDER BY occurredAtEpochMillis DESC
        """,
    )
    abstract fun observeWindow(
        windowStartEpochMillis: Long,
        nowEpochMillis: Long,
    ): Flow<List<EarthquakeEntity>>

    fun observeRecent(nowEpochMillis: Long): Flow<List<EarthquakeEntity>> = observeWindow(
        windowStartEpochMillis = nowEpochMillis - ROLLING_WINDOW_MILLIS,
        nowEpochMillis = nowEpochMillis,
    )

    @Query("SELECT * FROM earthquakes WHERE id = :id")
    abstract suspend fun findEarthquake(id: String): EarthquakeEntity?

    @Query("SELECT * FROM earthquakes ORDER BY occurredAtEpochMillis DESC")
    abstract suspend fun getAllEarthquakes(): List<EarthquakeEntity>

    @Upsert
    abstract suspend fun upsertEarthquakes(earthquakes: List<EarthquakeEntity>)

    @Query("DELETE FROM earthquakes")
    abstract suspend fun deleteAllEarthquakes()

    @Query("SELECT * FROM sync_metadata WHERE id = ${SyncMetadataEntity.SINGLETON_ID}")
    abstract suspend fun getSyncMetadata(): SyncMetadataEntity?

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
