package com.mlhysrszn.earthquake.domain.model

import java.time.Instant

/** One stored notification decision for an event, as shown in the activity log. */
data class NotificationDecision(
    /** Stable identity across USGS aliases; one decision per canonical event. */
    val canonicalEventId: String,
    val eventId: String,
    val occurredAt: Instant,
    /** Null once the event has left the 24-hour cache; decisions are kept for 30 days. */
    val magnitude: Double?,
    val place: String?,
    val outcome: NotificationDecisionOutcome,
    val decidedAt: Instant,
)

/** Why an event was or was not notified; mirrors the persisted processing outcomes. */
enum class NotificationDecisionOutcome {
    POSTED,
    PENDING,
    RETRYABLE,
    EXPIRED,
    BASELINED,
    SUPPRESSED_BELOW_THRESHOLD,
    SUPPRESSED_AT_THRESHOLD,
    SUPPRESSED_MISSING_MAGNITUDE,
    SUPPRESSED_DISABLED,
    SUPPRESSED_PERMISSION,
    SUPPRESSED_OUTSIDE_WINDOW,
    SUPPRESSED_ALREADY_PROCESSED,
    AMBIGUOUS_IDENTITY,
    UNKNOWN,
}
