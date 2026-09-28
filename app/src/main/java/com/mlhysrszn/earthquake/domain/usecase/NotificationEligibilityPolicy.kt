package com.mlhysrszn.earthquake.domain.usecase

import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

enum class NotificationSnapshotStatus {
    INITIAL_BASELINE,
    FRESH,
    STALE,
}

enum class NotificationIdentityStatus {
    NEW,
    PROCESSED,
    AMBIGUOUS_ALIAS,
}

sealed interface NotificationEligibilityDecision {
    data object Eligible : NotificationEligibilityDecision

    data class Suppressed(val reason: Reason) : NotificationEligibilityDecision

    enum class Reason {
        INITIAL_BASELINE,
        STALE_SNAPSHOT,
        AMBIGUOUS_IDENTITY,
        ALREADY_PROCESSED,
        OUTSIDE_WINDOW,
        DISABLED,
        PERMISSION_DENIED,
        MISSING_MAGNITUDE,
        BELOW_THRESHOLD,
        AT_THRESHOLD,
    }
}

data class NotificationEligibilityInput(
    val earthquake: Earthquake,
    val preferences: NotificationPreferences,
    val notificationPermissionGranted: Boolean,
    val snapshotStatus: NotificationSnapshotStatus,
    val identityStatus: NotificationIdentityStatus,
)

/** Pure notification eligibility rules; persistence and delivery remain separate adapters. */
class NotificationEligibilityPolicy @Inject constructor(
    private val clock: Clock,
) {
    fun evaluate(input: NotificationEligibilityInput): NotificationEligibilityDecision {
        if (input.snapshotStatus == NotificationSnapshotStatus.INITIAL_BASELINE) {
            return input.suppress(NotificationEligibilityDecision.Reason.INITIAL_BASELINE)
        }
        if (input.snapshotStatus == NotificationSnapshotStatus.STALE) {
            return input.suppress(NotificationEligibilityDecision.Reason.STALE_SNAPSHOT)
        }

        when (input.identityStatus) {
            NotificationIdentityStatus.AMBIGUOUS_ALIAS ->
                return input.suppress(NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY)

            NotificationIdentityStatus.PROCESSED ->
                return input.suppress(NotificationEligibilityDecision.Reason.ALREADY_PROCESSED)

            NotificationIdentityStatus.NEW -> Unit
        }

        val now = clock.instant()
        val oldestEligibleTime = now.minus(ELIGIBILITY_WINDOW)
        if (
            input.earthquake.occurredAt.isBefore(oldestEligibleTime) ||
            input.earthquake.occurredAt.isAfter(now)
        ) {
            return input.suppress(NotificationEligibilityDecision.Reason.OUTSIDE_WINDOW)
        }

        if (!input.preferences.notificationsEnabled) {
            return input.suppress(NotificationEligibilityDecision.Reason.DISABLED)
        }
        if (!input.notificationPermissionGranted) {
            return input.suppress(NotificationEligibilityDecision.Reason.PERMISSION_DENIED)
        }

        val magnitude = input.earthquake.magnitude
            ?: return input.suppress(NotificationEligibilityDecision.Reason.MISSING_MAGNITUDE)
        return when {
            magnitude < input.preferences.magnitudeThreshold ->
                input.suppress(NotificationEligibilityDecision.Reason.BELOW_THRESHOLD)

            magnitude == input.preferences.magnitudeThreshold ->
                input.suppress(NotificationEligibilityDecision.Reason.AT_THRESHOLD)

            else -> NotificationEligibilityDecision.Eligible
        }
    }

    private fun NotificationEligibilityInput.suppress(
        reason: NotificationEligibilityDecision.Reason,
    ) = NotificationEligibilityDecision.Suppressed(reason)

    private companion object {
        val ELIGIBILITY_WINDOW: Duration = Duration.ofHours(24)
    }
}
