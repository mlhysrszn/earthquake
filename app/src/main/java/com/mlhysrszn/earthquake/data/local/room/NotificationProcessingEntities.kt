package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_processing",
    indices = [
        Index(value = ["outcome"]),
        Index(value = ["lastSeenAtEpochMillis"]),
    ],
)
data class NotificationProcessingEntity(
    @PrimaryKey val canonicalEventId: String,
    val latestEventId: String,
    val eventOccurredAtEpochMillis: Long,
    val outcome: String,
    val reason: String?,
    val lastSeenAtEpochMillis: Long,
    val lastDecisionAtEpochMillis: Long,
)

@Entity(
    tableName = "notification_event_aliases",
    indices = [Index(value = ["canonicalEventId"])],
)
data class NotificationEventAliasEntity(
    @PrimaryKey val aliasId: String,
    val canonicalEventId: String,
    val lastSeenAtEpochMillis: Long,
)

@Entity(tableName = "notification_processing_metadata")
data class NotificationProcessingMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val baselineEstablished: Boolean,
    val lastProcessedFeedCursorEpochMillis: Long?,
    val lastEvaluatedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

enum class NotificationProcessingOutcome {
    BASELINED,
    PENDING,
    POSTED,
    RETRYABLE,
    EXPIRED,
    SUPPRESSED_DISABLED,
    SUPPRESSED_PERMISSION,
    SUPPRESSED_MISSING_MAGNITUDE,
    SUPPRESSED_BELOW_THRESHOLD,
    /** Legacy: written before the threshold became inclusive; kept for existing rows. */
    SUPPRESSED_AT_THRESHOLD,
    SUPPRESSED_OUTSIDE_WINDOW,
    SUPPRESSED_ALREADY_PROCESSED,
    AMBIGUOUS_IDENTITY,
}

/** A processing row joined with the cached event, when it is still cached. */
data class NotificationDecisionRow(
    val canonicalEventId: String,
    val eventId: String,
    val eventOccurredAtEpochMillis: Long,
    val outcome: String,
    val lastDecisionAtEpochMillis: Long,
    val magnitude: Double?,
    val place: String?,
)
