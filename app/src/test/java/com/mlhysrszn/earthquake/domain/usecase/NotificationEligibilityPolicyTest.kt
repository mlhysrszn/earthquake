package com.mlhysrszn.earthquake.domain.usecase

import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationEligibilityPolicyTest {
    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val policy = NotificationEligibilityPolicy(Clock.fixed(now, ZoneOffset.UTC))
    private val enabledPreferences = NotificationPreferences(
        notificationsEnabled = true,
        magnitudeThreshold = 4.0,
    )

    @Test
    fun `magnitude at or above the saved threshold is eligible`() {
        assertSuppressed(3.9, NotificationEligibilityDecision.Reason.BELOW_THRESHOLD)
        assertEquals(NotificationEligibilityDecision.Eligible, evaluate(event(magnitude = 4.0)))
        assertEquals(NotificationEligibilityDecision.Eligible, evaluate(event(magnitude = 4.1)))
    }

    @Test
    fun `revised events are evaluated like new events`() {
        assertEquals(
            NotificationEligibilityDecision.Eligible,
            evaluate(event(magnitude = 4.5), identity = NotificationIdentityStatus.REVISED),
        )
        assertSuppressed(
            magnitude = 3.5,
            reason = NotificationEligibilityDecision.Reason.BELOW_THRESHOLD,
            identity = NotificationIdentityStatus.REVISED,
        )
    }

    @Test
    fun `initial baseline and stale snapshots never notify`() {
        assertSuppressed(
            magnitude = 8.0,
            reason = NotificationEligibilityDecision.Reason.INITIAL_BASELINE,
            snapshot = NotificationSnapshotStatus.INITIAL_BASELINE,
        )
        assertSuppressed(
            magnitude = 8.0,
            reason = NotificationEligibilityDecision.Reason.STALE_SNAPSHOT,
            snapshot = NotificationSnapshotStatus.STALE,
        )
    }

    @Test
    fun `duplicates revisions and aliases of processed identities never notify`() {
        assertSuppressed(
            magnitude = 8.0,
            reason = NotificationEligibilityDecision.Reason.ALREADY_PROCESSED,
            identity = NotificationIdentityStatus.PROCESSED,
        )
        assertSuppressed(
            magnitude = 8.0,
            reason = NotificationEligibilityDecision.Reason.ALREADY_PROCESSED,
            identity = NotificationIdentityStatus.PROCESSED,
        )
        assertSuppressed(
            magnitude = 8.0,
            reason = NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY,
            identity = NotificationIdentityStatus.AMBIGUOUS_ALIAS,
        )
    }

    @Test
    fun `threshold changes affect new events but never replay a processed event`() {
        val event = event(magnitude = 5.0)
        assertEquals(
            NotificationEligibilityDecision.Suppressed(
                NotificationEligibilityDecision.Reason.BELOW_THRESHOLD,
            ),
            evaluate(event, preferences(threshold = 6.0)),
        )
        assertEquals(
            NotificationEligibilityDecision.Eligible,
            evaluate(event, preferences(threshold = 4.0)),
        )
        assertEquals(
            NotificationEligibilityDecision.Suppressed(
                NotificationEligibilityDecision.Reason.ALREADY_PROCESSED,
            ),
            evaluate(
                event,
                preferences(threshold = 3.0),
                identity = NotificationIdentityStatus.PROCESSED,
            ),
        )
    }

    @Test
    fun `late arrivals inside rolling window qualify but old or future events do not`() {
        // A fresh snapshot may first reveal an event whose occurrence predates the prior cursor.
        val lateButRecent = event(
            magnitude = 4.1,
            occurredAt = now.minus(Duration.ofHours(23)),
        )
        assertEquals(NotificationEligibilityDecision.Eligible, evaluate(lateButRecent))

        assertSuppressed(
            magnitude = 4.1,
            occurredAt = now.minus(Duration.ofHours(24)).minusMillis(1),
            reason = NotificationEligibilityDecision.Reason.OUTSIDE_WINDOW,
        )
        assertSuppressed(
            magnitude = 4.1,
            occurredAt = now.plusMillis(1),
            reason = NotificationEligibilityDecision.Reason.OUTSIDE_WINDOW,
        )
        assertEquals(
            NotificationEligibilityDecision.Eligible,
            evaluate(event(magnitude = 4.1, occurredAt = now.minus(Duration.ofHours(24)))),
        )
    }

    @Test
    fun `disabled preference and denied OS permission suppress an event`() {
        assertSuppressed(
            magnitude = 5.0,
            reason = NotificationEligibilityDecision.Reason.DISABLED,
            preferences = preferences(enabled = false),
        )
        assertSuppressed(
            magnitude = 5.0,
            reason = NotificationEligibilityDecision.Reason.PERMISSION_DENIED,
            permissionGranted = false,
        )
    }

    @Test
    fun `missing magnitude is not eligible`() {
        assertSuppressed(
            magnitude = null,
            reason = NotificationEligibilityDecision.Reason.MISSING_MAGNITUDE,
        )
    }

    private fun evaluate(
        earthquake: Earthquake,
        preferences: NotificationPreferences = enabledPreferences,
        permissionGranted: Boolean = true,
        snapshot: NotificationSnapshotStatus = NotificationSnapshotStatus.FRESH,
        identity: NotificationIdentityStatus = NotificationIdentityStatus.NEW,
    ): NotificationEligibilityDecision = policy.evaluate(
        NotificationEligibilityInput(
            earthquake = earthquake,
            preferences = preferences,
            notificationPermissionGranted = permissionGranted,
            snapshotStatus = snapshot,
            identityStatus = identity,
        ),
    )

    private fun assertSuppressed(
        magnitude: Double?,
        reason: NotificationEligibilityDecision.Reason,
        occurredAt: Instant = now.minusSeconds(60),
        preferences: NotificationPreferences = enabledPreferences,
        permissionGranted: Boolean = true,
        snapshot: NotificationSnapshotStatus = NotificationSnapshotStatus.FRESH,
        identity: NotificationIdentityStatus = NotificationIdentityStatus.NEW,
    ) {
        assertEquals(
            NotificationEligibilityDecision.Suppressed(reason),
            evaluate(
                earthquake = event(magnitude = magnitude, occurredAt = occurredAt),
                preferences = preferences,
                permissionGranted = permissionGranted,
                snapshot = snapshot,
                identity = identity,
            ),
        )
    }

    private fun preferences(
        enabled: Boolean = true,
        threshold: Double = NotificationPreferences.DEFAULT_MAGNITUDE_THRESHOLD,
    ) = NotificationPreferences(
        notificationsEnabled = enabled,
        magnitudeThreshold = threshold,
    )

    private fun event(
        magnitude: Double?,
        occurredAt: Instant = now.minusSeconds(60),
    ) = Earthquake(
        id = "policy-event",
        magnitude = magnitude,
        magnitudeType = null,
        place = "Near the coast",
        occurredAt = occurredAt,
        updatedAt = null,
        longitude = null,
        latitude = null,
        depthKm = null,
        sourceUrl = null,
    )
}
