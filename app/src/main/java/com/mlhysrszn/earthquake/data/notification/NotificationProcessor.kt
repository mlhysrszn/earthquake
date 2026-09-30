package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.EarthquakeEntity
import com.mlhysrszn.earthquake.data.local.room.NotificationEventAliasEntity
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingDao
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingEntity
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingMetadataEntity
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingOutcome
import com.mlhysrszn.earthquake.data.local.room.toDomain
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.usecase.NotificationEligibilityDecision
import com.mlhysrszn.earthquake.domain.usecase.NotificationEligibilityInput
import com.mlhysrszn.earthquake.domain.usecase.NotificationEligibilityPolicy
import com.mlhysrszn.earthquake.domain.usecase.NotificationIdentityStatus
import com.mlhysrszn.earthquake.domain.usecase.NotificationSnapshotStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PendingNotificationCandidate(
    val canonicalEventId: String,
    val eventId: String,
    val occurredAt: Instant,
)

data class NotificationProcessingBatch(
    val snapshotStatus: NotificationSnapshotStatus?,
    val pendingNotifications: List<PendingNotificationCandidate>,
)

/**
 * Turns the latest stored snapshot into persisted notification decisions before delivery.
 *
 * One run: skip snapshots already processed, resolve each event's identity (ID plus USGS
 * aliases), ask [NotificationEligibilityPolicy] for a decision, and commit every decision
 * with the new feed cursor in one transaction. [PendingNotificationDispatcher] then delivers
 * the PENDING rows and reports back through the `mark*` functions.
 */
@Singleton
class NotificationProcessor @Inject constructor(
    private val earthquakeDao: EarthquakeDao,
    private val processingDao: NotificationProcessingDao,
    private val preferencesRepository: NotificationPreferencesRepository,
    private val eligibilityPolicy: NotificationEligibilityPolicy,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    /** Pending IDs handed to a dispatcher in this process and not yet resolved. */
    private val emittedPendingIds = mutableSetOf<String>()

    suspend fun processLatestSnapshot(
        notificationPermissionGranted: Boolean,
    ): NotificationProcessingBatch = mutex.withLock {
        val now = clock.instant()
        removeExpiredState(now)

        // Another writer may commit between our read and write; retry on a changed cursor.
        repeat(MAX_STATE_RETRIES) {
            val snapshot = earthquakeDao.loadPersistedSnapshot()
            val syncMetadata = snapshot.syncMetadata
                ?: return@withLock pendingBatch(snapshotStatus = null, now = now)
            val snapshotCursor = syncMetadata.sourceGeneratedAtEpochMillis
                ?: syncMetadata.lastSuccessfulFetchAtEpochMillis
            val expectedMetadata = processingDao.getMetadata()
            val snapshotStatus = snapshotStatus(expectedMetadata, snapshotCursor)
            if (snapshotStatus == NotificationSnapshotStatus.STALE) {
                return@withLock pendingBatch(snapshotStatus, now)
            }

            val decisions = decideSnapshot(
                earthquakes = snapshot.earthquakes,
                snapshotStatus = snapshotStatus,
                notificationPermissionGranted = notificationPermissionGranted,
                now = now,
            )
            val committed = processingDao.commitSnapshotDecisions(
                expectedMetadata = expectedMetadata,
                newMetadata = NotificationProcessingMetadataEntity(
                    baselineEstablished = true,
                    lastProcessedFeedCursorEpochMillis = snapshotCursor,
                    lastEvaluatedAtEpochMillis = now.toEpochMilli(),
                ),
                eventStates = decisions.states,
                aliases = decisions.aliases,
                retainedSinceEpochMillis = retainedSince(now),
            )
            if (committed) return@withLock pendingBatch(snapshotStatus, now)
        }

        pendingBatch(snapshotStatus = NotificationSnapshotStatus.STALE, now = now)
    }

    suspend fun markPosted(canonicalEventId: String): Boolean =
        resolvePending(canonicalEventId, NotificationProcessingOutcome.POSTED)

    suspend fun markRetryable(canonicalEventId: String): Boolean =
        resolvePending(canonicalEventId, NotificationProcessingOutcome.RETRYABLE)

    suspend fun markPermissionDenied(canonicalEventId: String): Boolean =
        resolvePending(canonicalEventId, NotificationProcessingOutcome.SUPPRESSED_PERMISSION)

    suspend fun markExpired(canonicalEventId: String): Boolean =
        resolvePending(canonicalEventId, NotificationProcessingOutcome.EXPIRED)

    /** Releases this process's in-flight reservation if delivery exits unexpectedly. */
    suspend fun releasePending(canonicalEventId: String) = mutex.withLock {
        emittedPendingIds.remove(canonicalEventId)
    }

    private suspend fun removeExpiredState(now: Instant) {
        val nowEpochMillis = now.toEpochMilli()
        processingDao.expireOldPending(nowEpochMillis - EVENT_WINDOW.toMillis(), nowEpochMillis)
        processingDao.pruneTerminalOutcomes(retainedSince(now))
        processingDao.pruneOrphanedAliases()
    }

    private fun snapshotStatus(
        metadata: NotificationProcessingMetadataEntity?,
        snapshotCursor: Long,
    ): NotificationSnapshotStatus {
        val lastCursor = metadata?.lastProcessedFeedCursorEpochMillis
        return when {
            metadata?.baselineEstablished != true -> NotificationSnapshotStatus.INITIAL_BASELINE
            lastCursor == null || snapshotCursor <= lastCursor -> NotificationSnapshotStatus.STALE
            else -> NotificationSnapshotStatus.FRESH
        }
    }

    /** Decides every event of the snapshot that is inside the 24-hour window, newest first. */
    private suspend fun decideSnapshot(
        earthquakes: List<EarthquakeEntity>,
        snapshotStatus: NotificationSnapshotStatus,
        notificationPermissionGranted: Boolean,
        now: Instant,
    ): SnapshotDecisions {
        val nowEpochMillis = now.toEpochMilli()
        val windowStartEpochMillis = nowEpochMillis - EVENT_WINDOW.toMillis()
        val preferences = preferencesRepository.observePreferences().first()
        val statesById = processingDao.getAllEventStates()
            .associateByTo(mutableMapOf(), NotificationProcessingEntity::canonicalEventId)
        val identities = IdentityIndex(processingDao.getAllAliases())
        val changedStates = linkedMapOf<String, NotificationProcessingEntity>()

        earthquakes
            .filter { entity -> entity.occurredAtEpochMillis in windowStartEpochMillis..nowEpochMillis }
            .map { entity -> entity.toDomain() }
            .sortedByDescending(Earthquake::occurredAt)
            .forEach { earthquake ->
                val identity = identities.resolve(earthquake)
                val previous = statesById[identity.canonicalId]
                val identityStatus = identityStatus(identity, previous, earthquake)
                val decision = eligibilityPolicy.evaluate(
                    NotificationEligibilityInput(
                        earthquake = earthquake,
                        preferences = preferences,
                        notificationPermissionGranted = notificationPermissionGranted,
                        snapshotStatus = snapshotStatus,
                        identityStatus = identityStatus,
                    ),
                )
                val next = nextState(
                    previous = previous,
                    earthquake = earthquake,
                    canonicalId = identity.canonicalId,
                    identityStatus = identityStatus,
                    decision = decision,
                    nowEpochMillis = nowEpochMillis,
                )
                statesById[next.canonicalEventId] = next
                changedStates[next.canonicalEventId] = next
                if (!identity.ambiguous) identities.remember(identity, nowEpochMillis)
            }

        return SnapshotDecisions(
            states = changedStates.values.toList(),
            aliases = identities.changedAliases(),
        )
    }

    private fun identityStatus(
        identity: ResolvedIdentity,
        previous: NotificationProcessingEntity?,
        earthquake: Earthquake,
    ): NotificationIdentityStatus = when {
        identity.ambiguous -> NotificationIdentityStatus.AMBIGUOUS_ALIAS
        previous == null -> NotificationIdentityStatus.NEW
        previous.isRevisedAfterSuppression(earthquake) -> NotificationIdentityStatus.REVISED
        else -> NotificationIdentityStatus.PROCESSED
    }

    /** A source update after a below-threshold or missing-magnitude decision is decided again. */
    private fun NotificationProcessingEntity.isRevisedAfterSuppression(earthquake: Earthquake): Boolean =
        outcome in REEVALUATED_OUTCOMES &&
            (earthquake.updatedAt?.toEpochMilli() ?: Long.MIN_VALUE) > lastDecisionAtEpochMillis

    /** The stored state after this record; only NEW and REVISED records take the policy decision. */
    private fun nextState(
        previous: NotificationProcessingEntity?,
        earthquake: Earthquake,
        canonicalId: String,
        identityStatus: NotificationIdentityStatus,
        decision: NotificationEligibilityDecision,
        nowEpochMillis: Long,
    ): NotificationProcessingEntity = when (identityStatus) {
        NotificationIdentityStatus.AMBIGUOUS_ALIAS ->
            previous?.observed(earthquake, nowEpochMillis)?.copy(
                // Never deliver a pending notification whose identity became ambiguous.
                outcome = if (previous.outcome in PENDING_OUTCOMES) {
                    NotificationProcessingOutcome.AMBIGUOUS_IDENTITY.name
                } else {
                    previous.outcome
                },
                reason = NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY.name,
                lastDecisionAtEpochMillis = nowEpochMillis,
            ) ?: newState(
                canonicalId = canonicalId,
                earthquake = earthquake,
                outcome = NotificationProcessingOutcome.AMBIGUOUS_IDENTITY,
                reason = NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY,
                nowEpochMillis = nowEpochMillis,
            )

        NotificationIdentityStatus.REVISED ->
            checkNotNull(previous).observed(earthquake, nowEpochMillis).copy(
                outcome = decision.toOutcome().name,
                reason = decision.suppressionReason()?.name,
                lastDecisionAtEpochMillis = nowEpochMillis,
            )

        NotificationIdentityStatus.PROCESSED ->
            checkNotNull(previous).observed(earthquake, nowEpochMillis)

        NotificationIdentityStatus.NEW -> newState(
            canonicalId = canonicalId,
            earthquake = earthquake,
            outcome = decision.toOutcome(),
            reason = decision.suppressionReason(),
            nowEpochMillis = nowEpochMillis,
        )
    }

    private suspend fun resolvePending(
        canonicalEventId: String,
        outcome: NotificationProcessingOutcome,
    ): Boolean = mutex.withLock {
        val changed = processingDao.updatePendingOutcome(
            canonicalEventId = canonicalEventId,
            outcome = outcome.name,
            nowEpochMillis = clock.millis(),
        ) > 0
        if (changed) emittedPendingIds.remove(canonicalEventId)
        changed
    }

    /** Pending rows inside the window that no dispatcher in this process is handling yet. */
    private suspend fun pendingBatch(
        snapshotStatus: NotificationSnapshotStatus?,
        now: Instant,
    ): NotificationProcessingBatch {
        val oldestEligibleTime = now.minus(EVENT_WINDOW)
        val pending = processingDao.getPendingNotifications()
            .filter { entity ->
                val occurredAt = Instant.ofEpochMilli(entity.eventOccurredAtEpochMillis)
                !occurredAt.isBefore(oldestEligibleTime) && !occurredAt.isAfter(now)
            }
            .filterNot { entity -> entity.canonicalEventId in emittedPendingIds }
            .map { entity ->
                PendingNotificationCandidate(
                    canonicalEventId = entity.canonicalEventId,
                    eventId = entity.latestEventId,
                    occurredAt = Instant.ofEpochMilli(entity.eventOccurredAtEpochMillis),
                )
            }
        emittedPendingIds += pending.map(PendingNotificationCandidate::canonicalEventId)
        return NotificationProcessingBatch(snapshotStatus, pending)
    }

    /** Records that the event was seen again, keeping the newest record as the latest ID. */
    private fun NotificationProcessingEntity.observed(
        earthquake: Earthquake,
        nowEpochMillis: Long,
    ): NotificationProcessingEntity {
        val occurredAtEpochMillis = earthquake.occurredAt.toEpochMilli()
        val isNewerRecord = occurredAtEpochMillis > eventOccurredAtEpochMillis
        return copy(
            latestEventId = if (isNewerRecord) earthquake.id else latestEventId,
            eventOccurredAtEpochMillis = maxOf(occurredAtEpochMillis, eventOccurredAtEpochMillis),
            lastSeenAtEpochMillis = nowEpochMillis,
        )
    }

    private fun newState(
        canonicalId: String,
        earthquake: Earthquake,
        outcome: NotificationProcessingOutcome,
        reason: NotificationEligibilityDecision.Reason?,
        nowEpochMillis: Long,
    ) = NotificationProcessingEntity(
        canonicalEventId = canonicalId,
        latestEventId = earthquake.id,
        eventOccurredAtEpochMillis = earthquake.occurredAt.toEpochMilli(),
        outcome = outcome.name,
        reason = reason?.name,
        lastSeenAtEpochMillis = nowEpochMillis,
        lastDecisionAtEpochMillis = nowEpochMillis,
    )

    private fun retainedSince(now: Instant): Long =
        now.toEpochMilli() - PROCESSING_HISTORY_RETENTION.toMillis()

    private fun NotificationEligibilityDecision.suppressionReason() =
        (this as? NotificationEligibilityDecision.Suppressed)?.reason

    private fun NotificationEligibilityDecision.toOutcome(): NotificationProcessingOutcome =
        when (suppressionReason()) {
            null -> NotificationProcessingOutcome.PENDING
            NotificationEligibilityDecision.Reason.INITIAL_BASELINE -> NotificationProcessingOutcome.BASELINED
            NotificationEligibilityDecision.Reason.STALE_SNAPSHOT,
            NotificationEligibilityDecision.Reason.ALREADY_PROCESSED,
            -> NotificationProcessingOutcome.SUPPRESSED_ALREADY_PROCESSED
            NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY -> NotificationProcessingOutcome.AMBIGUOUS_IDENTITY
            NotificationEligibilityDecision.Reason.OUTSIDE_WINDOW -> NotificationProcessingOutcome.SUPPRESSED_OUTSIDE_WINDOW
            NotificationEligibilityDecision.Reason.DISABLED -> NotificationProcessingOutcome.SUPPRESSED_DISABLED
            NotificationEligibilityDecision.Reason.PERMISSION_DENIED -> NotificationProcessingOutcome.SUPPRESSED_PERMISSION
            NotificationEligibilityDecision.Reason.MISSING_MAGNITUDE ->
                NotificationProcessingOutcome.SUPPRESSED_MISSING_MAGNITUDE
            NotificationEligibilityDecision.Reason.BELOW_THRESHOLD ->
                NotificationProcessingOutcome.SUPPRESSED_BELOW_THRESHOLD
        }

    private data class SnapshotDecisions(
        val states: List<NotificationProcessingEntity>,
        val aliases: List<NotificationEventAliasEntity>,
    )

    private companion object {
        const val MAX_STATE_RETRIES = 3
        val EVENT_WINDOW: Duration = Duration.ofHours(24)
        val PROCESSING_HISTORY_RETENTION: Duration = Duration.ofDays(30)

        /** Outcomes that a later source revision may change; at-threshold rows predate the >= rule. */
        val REEVALUATED_OUTCOMES = setOf(
            NotificationProcessingOutcome.SUPPRESSED_BELOW_THRESHOLD.name,
            NotificationProcessingOutcome.SUPPRESSED_AT_THRESHOLD.name,
            NotificationProcessingOutcome.SUPPRESSED_MISSING_MAGNITUDE.name,
        )
        val PENDING_OUTCOMES = setOf(
            NotificationProcessingOutcome.PENDING.name,
            NotificationProcessingOutcome.RETRYABLE.name,
        )
    }
}
