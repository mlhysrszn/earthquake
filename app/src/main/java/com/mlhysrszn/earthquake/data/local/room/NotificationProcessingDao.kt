package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NotificationProcessingDao {
    @Query("SELECT * FROM notification_processing_metadata WHERE id = ${NotificationProcessingMetadataEntity.SINGLETON_ID}")
    abstract suspend fun getMetadata(): NotificationProcessingMetadataEntity?

    @Query("SELECT * FROM notification_processing WHERE canonicalEventId = :canonicalEventId")
    abstract suspend fun getEventState(canonicalEventId: String): NotificationProcessingEntity?

    @Query("SELECT * FROM notification_processing")
    abstract suspend fun getAllEventStates(): List<NotificationProcessingEntity>

    @Query(
        """
        SELECT p.canonicalEventId, p.latestEventId AS eventId, p.eventOccurredAtEpochMillis, p.outcome,
            p.lastDecisionAtEpochMillis, e.magnitude, e.place
        FROM notification_processing p
        LEFT JOIN earthquakes e ON e.id = p.latestEventId
        ORDER BY p.lastDecisionAtEpochMillis DESC, p.eventOccurredAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    abstract fun observeRecentDecisions(limit: Int): Flow<List<NotificationDecisionRow>>

    @Query("SELECT * FROM notification_event_aliases")
    abstract suspend fun getAllAliases(): List<NotificationEventAliasEntity>

    @Query(
        """
        SELECT * FROM notification_processing
        WHERE outcome IN ('PENDING', 'RETRYABLE')
        ORDER BY lastSeenAtEpochMillis ASC
        """,
    )
    abstract suspend fun getPendingNotifications(): List<NotificationProcessingEntity>

    @Upsert
    abstract suspend fun upsertEventStates(states: List<NotificationProcessingEntity>)

    @Upsert
    abstract suspend fun upsertAliases(aliases: List<NotificationEventAliasEntity>)

    @Upsert
    abstract suspend fun upsertMetadata(metadata: NotificationProcessingMetadataEntity)

    @Query(
        """
        UPDATE notification_processing
        SET outcome = 'EXPIRED', lastDecisionAtEpochMillis = :nowEpochMillis
        WHERE outcome IN ('PENDING', 'RETRYABLE')
          AND (eventOccurredAtEpochMillis < :windowStartEpochMillis
            OR eventOccurredAtEpochMillis > :nowEpochMillis)
        """,
    )
    abstract suspend fun expireOldPending(
        windowStartEpochMillis: Long,
        nowEpochMillis: Long,
    )

    @Query(
        """
        UPDATE notification_processing
        SET outcome = :outcome, lastDecisionAtEpochMillis = :nowEpochMillis
        WHERE canonicalEventId = :canonicalEventId
          AND outcome IN ('PENDING', 'RETRYABLE')
        """,
    )
    abstract suspend fun updatePendingOutcome(
        canonicalEventId: String,
        outcome: String,
        nowEpochMillis: Long,
    ): Int

    @Query(
        """
        DELETE FROM notification_processing
        WHERE lastSeenAtEpochMillis < :retainedSinceEpochMillis
          AND outcome NOT IN ('PENDING', 'RETRYABLE')
        """,
    )
    abstract suspend fun pruneTerminalOutcomes(retainedSinceEpochMillis: Long)

    @Query(
        """
        DELETE FROM notification_event_aliases
        WHERE canonicalEventId NOT IN (SELECT canonicalEventId FROM notification_processing)
        """,
    )
    abstract suspend fun pruneOrphanedAliases()

    /** Cursor compare-and-set and all event/alias outcomes commit in one Room transaction. */
    @Transaction
    open suspend fun commitSnapshotDecisions(
        expectedMetadata: NotificationProcessingMetadataEntity?,
        newMetadata: NotificationProcessingMetadataEntity,
        eventStates: List<NotificationProcessingEntity>,
        aliases: List<NotificationEventAliasEntity>,
        retainedSinceEpochMillis: Long,
    ): Boolean {
        if (getMetadata() != expectedMetadata) return false

        if (eventStates.isNotEmpty()) upsertEventStates(eventStates)
        if (aliases.isNotEmpty()) upsertAliases(aliases)
        upsertMetadata(newMetadata)
        pruneTerminalOutcomes(retainedSinceEpochMillis)
        pruneOrphanedAliases()
        return true
    }
}
