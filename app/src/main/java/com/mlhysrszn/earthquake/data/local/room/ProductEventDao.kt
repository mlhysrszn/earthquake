package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductEventDao {
    @Insert
    protected abstract suspend fun insert(event: ProductEventEntity)

    @Query(
        "SELECT * FROM product_events ORDER BY occurredAtEpochMillis DESC, id DESC LIMIT :limit",
    )
    abstract fun observeRecent(limit: Int): Flow<List<ProductEventEntity>>

    @Query(
        "SELECT * FROM product_events ORDER BY occurredAtEpochMillis DESC, id DESC LIMIT :limit",
    )
    abstract suspend fun getRecent(limit: Int): List<ProductEventEntity>

    @Query("DELETE FROM product_events WHERE occurredAtEpochMillis < :retainedSinceEpochMillis")
    protected abstract suspend fun deleteOlderThan(retainedSinceEpochMillis: Long)

    @Query(
        """
        DELETE FROM product_events
        WHERE id NOT IN (
            SELECT id FROM product_events
            ORDER BY occurredAtEpochMillis DESC, id DESC
            LIMIT :maximumRows
        )
        """,
    )
    protected abstract suspend fun trimToMaximumRows(maximumRows: Int)

    @Transaction
    open suspend fun record(event: ProductEventEntity, retainedSinceEpochMillis: Long) {
        insert(event)
        deleteOlderThan(retainedSinceEpochMillis)
        trimToMaximumRows(MAXIMUM_EVENT_ROWS)
    }

    private companion object {
        const val MAXIMUM_EVENT_ROWS = 20_000
    }
}
