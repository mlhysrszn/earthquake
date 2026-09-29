package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
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

/** Persists baseline, identity, and eligibility decisions before notification delivery is attempted. */
@Singleton
class NotificationProcessor @Inject constructor(
    private val earthquakeDao: EarthquakeDao,
    private val processingDao: NotificationProcessingDao,
    private val preferencesRepository: NotificationPreferencesRepository,
    private val eligibilityPolicy: NotificationEligibilityPolicy,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private val emittedPendingIds = mutableSetOf<String>()

    suspend fun processLatestSnapshot(
        notificationPermissionGranted: Boolean,
    ): NotificationProcessingBatch = mutex.withLock {
        val now = clock.instant()
        val nowEpochMillis = now.toEpochMilli()
        val windowStartEpochMillis = nowEpochMillis - EVENT_WINDOW.toMillis()
        val retainedSinceEpochMillis = nowEpochMillis - PROCESSING_HISTORY_RETENTION.toMillis()

        processingDao.expireOldPending(windowStartEpochMillis, nowEpochMillis)
        processingDao.pruneTerminalOutcomes(retainedSinceEpochMillis)
        processingDao.pruneOrphanedAliases()

        repeat(MAX_STATE_RETRIES) {
            val snapshot = earthquakeDao.loadPersistedSnapshot()
            val syncMetadata = snapshot.syncMetadata
                ?: return@withLock pendingBatch(snapshotStatus = null, now = now)
            val expectedMetadata = processingDao.getMetadata()
            val snapshotCursor = syncMetadata.sourceGeneratedAtEpochMillis
                ?: syncMetadata.lastSuccessfulFetchAtEpochMillis
            val snapshotStatus = when {
                expectedMetadata?.baselineEstablished != true ->
                    NotificationSnapshotStatus.INITIAL_BASELINE

                expectedMetadata.lastProcessedFeedCursorEpochMillis == null ||
                    snapshotCursor <= expectedMetadata.lastProcessedFeedCursorEpochMillis ->
                    NotificationSnapshotStatus.STALE

                else -> NotificationSnapshotStatus.FRESH
            }

            if (snapshotStatus == NotificationSnapshotStatus.STALE) {
                return@withLock pendingBatch(snapshotStatus, now)
            }

            val preferences = preferencesRepository.observePreferences().first()
            val eventStatesByCanonicalId = processingDao.getAllEventStates()
                .associateBy(NotificationProcessingEntity::canonicalEventId)
                .toMutableMap()
            val aliasToCanonicalId = processingDao.getAllAliases()
                .associate { alias -> alias.aliasId to alias.canonicalEventId }
                .toMutableMap()
            val statesToWrite = linkedMapOf<String, NotificationProcessingEntity>()
            val aliasesToWrite = linkedMapOf<String, NotificationEventAliasEntity>()

            snapshot.earthquakes
                .asSequence()
                .filter { entity ->
                    entity.occurredAtEpochMillis in windowStartEpochMillis..nowEpochMillis
                }
                .map { entity -> entity.toDomain() }
                .sortedByDescending(Earthquake::occurredAt)
                .forEach { earthquake ->
                    val identityTokens = (earthquake.aliases + earthquake.id)
                        .asSequence()
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .toSortedSet()
                    val resolvedCanonicalIds = identityTokens.mapNotNull(aliasToCanonicalId::get).toSet()
                    val ambiguousAliases = resolvedCanonicalIds.size > 1
                    val canonicalId = resolvedCanonicalIds.singleOrNull() ?: earthquake.id
                    val previousState = statesToWrite[canonicalId]
                        ?: eventStatesByCanonicalId[canonicalId]
                    val identityStatus = when {
                        ambiguousAliases -> NotificationIdentityStatus.AMBIGUOUS_ALIAS
                        previousState != null -> NotificationIdentityStatus.PROCESSED
                        else -> NotificationIdentityStatus.NEW
                    }

                    val decision = eligibilityPolicy.evaluate(
                        NotificationEligibilityInput(
                            earthquake = earthquake,
                            preferences = preferences,
                            notificationPermissionGranted = notificationPermissionGranted,
                            snapshotStatus = snapshotStatus,
                            identityStatus = identityStatus,
                        ),
                    )
                    val processingState = when {
                        ambiguousAliases && previousState != null -> {
                            val occurredAtEpochMillis = earthquake.occurredAt.toEpochMilli()
                            val isNewerRecord =
                                occurredAtEpochMillis > previousState.eventOccurredAtEpochMillis
                            previousState.copy(
                                latestEventId = if (isNewerRecord) {
                                    earthquake.id
                                } else {
                                    previousState.latestEventId
                                },
                                eventOccurredAtEpochMillis = if (isNewerRecord) {
                                    occurredAtEpochMillis
                                } else {
                                    previousState.eventOccurredAtEpochMillis
                                },
                                outcome = if (previousState.outcome in PENDING_OUTCOMES) {
                                    NotificationProcessingOutcome.AMBIGUOUS_IDENTITY.name
                                } else {
                                    previousState.outcome
                                },
                                reason = NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY.name,
                                lastSeenAtEpochMillis = nowEpochMillis,
                                lastDecisionAtEpochMillis = nowEpochMillis,
                            )
                        }

                        previousState != null -> {
                            val occurredAtEpochMillis = earthquake.occurredAt.toEpochMilli()
                            val isNewerRecord =
                                occurredAtEpochMillis > previousState.eventOccurredAtEpochMillis
                            previousState.copy(
                                latestEventId = if (isNewerRecord) {
                                    earthquake.id
                                } else {
                                    previousState.latestEventId
                                },
                                eventOccurredAtEpochMillis = if (isNewerRecord) {
                                    occurredAtEpochMillis
                                } else {
                                    previousState.eventOccurredAtEpochMillis
                                },
                                lastSeenAtEpochMillis = nowEpochMillis,
                            )
                        }

                        ambiguousAliases -> processingState(
                            canonicalEventId = earthquake.id,
                            earthquake = earthquake,
                            outcome = NotificationProcessingOutcome.AMBIGUOUS_IDENTITY,
                            reason = NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY,
                            nowEpochMillis = nowEpochMillis,
                        )

                        snapshotStatus == NotificationSnapshotStatus.INITIAL_BASELINE ->
                            processingState(
                                canonicalEventId = canonicalId,
                                earthquake = earthquake,
                                outcome = NotificationProcessingOutcome.BASELINED,
                                reason = NotificationEligibilityDecision.Reason.INITIAL_BASELINE,
                                nowEpochMillis = nowEpochMillis,
                            )

                        else -> processingState(
                            canonicalEventId = canonicalId,
                            earthquake = earthquake,
                            outcome = decision.toOutcome(),
                            reason = (decision as? NotificationEligibilityDecision.Suppressed)?.reason,
                            nowEpochMillis = nowEpochMillis,
                        )
                    }
                    statesToWrite[processingState.canonicalEventId] = processingState
                    eventStatesByCanonicalId[processingState.canonicalEventId] = processingState

                    if (!ambiguousAliases) {
                        identityTokens.forEach { aliasId ->
                            val knownCanonicalId = aliasToCanonicalId[aliasId]
                            if (knownCanonicalId == null || knownCanonicalId == canonicalId) {
                                aliasToCanonicalId[aliasId] = canonicalId
                                aliasesToWrite[aliasId] = NotificationEventAliasEntity(
                                    aliasId = aliasId,
                                    canonicalEventId = canonicalId,
                                    lastSeenAtEpochMillis = nowEpochMillis,
                                )
                            }
                        }
                    }
                }

            val newMetadata = NotificationProcessingMetadataEntity(
                baselineEstablished = true,
                lastProcessedFeedCursorEpochMillis = snapshotCursor,
                lastEvaluatedAtEpochMillis = nowEpochMillis,
            )
            val committed = processingDao.commitSnapshotDecisions(
                expectedMetadata = expectedMetadata,
                newMetadata = newMetadata,
                eventStates = statesToWrite.values.toList(),
                aliases = aliasesToWrite.values.toList(),
                retainedSinceEpochMillis = retainedSinceEpochMillis,
            )
            if (committed) return@withLock pendingBatch(snapshotStatus, now)
        }

        pendingBatch(snapshotStatus = NotificationSnapshotStatus.STALE, now = now)
    }

    suspend fun markPosted(canonicalEventId: String): Boolean = mutex.withLock {
        val changed = processingDao.updatePendingOutcome(
            canonicalEventId = canonicalEventId,
            outcome = NotificationProcessingOutcome.POSTED.name,
            nowEpochMillis = clock.millis(),
        ) > 0
        if (changed) emittedPendingIds.remove(canonicalEventId)
        changed
    }

    suspend fun markRetryable(canonicalEventId: String): Boolean = mutex.withLock {
        val changed = processingDao.updatePendingOutcome(
            canonicalEventId = canonicalEventId,
            outcome = NotificationProcessingOutcome.RETRYABLE.name,
            nowEpochMillis = clock.millis(),
        ) > 0
        if (changed) emittedPendingIds.remove(canonicalEventId)
        changed
    }

    suspend fun markPermissionDenied(canonicalEventId: String): Boolean = mutex.withLock {
        val changed = processingDao.updatePendingOutcome(
            canonicalEventId = canonicalEventId,
            outcome = NotificationProcessingOutcome.SUPPRESSED_PERMISSION.name,
            nowEpochMillis = clock.millis(),
        ) > 0
        if (changed) emittedPendingIds.remove(canonicalEventId)
        changed
    }

    suspend fun markExpired(canonicalEventId: String): Boolean = mutex.withLock {
        val changed = processingDao.updatePendingOutcome(
            canonicalEventId = canonicalEventId,
            outcome = NotificationProcessingOutcome.EXPIRED.name,
            nowEpochMillis = clock.millis(),
        ) > 0
        if (changed) emittedPendingIds.remove(canonicalEventId)
        changed
    }

    /** Releases this process's in-flight reservation if delivery exits unexpectedly. */
    suspend fun releasePending(canonicalEventId: String) = mutex.withLock {
        emittedPendingIds.remove(canonicalEventId)
    }

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

    private fun processingState(
        canonicalEventId: String,
        earthquake: Earthquake,
        outcome: NotificationProcessingOutcome,
        reason: NotificationEligibilityDecision.Reason?,
        nowEpochMillis: Long,
    ) = NotificationProcessingEntity(
        canonicalEventId = canonicalEventId,
        latestEventId = earthquake.id,
        eventOccurredAtEpochMillis = earthquake.occurredAt.toEpochMilli(),
        outcome = outcome.name,
        reason = reason?.name,
        lastSeenAtEpochMillis = nowEpochMillis,
        lastDecisionAtEpochMillis = nowEpochMillis,
    )

    private fun NotificationEligibilityDecision.toOutcome(): NotificationProcessingOutcome = when (this) {
        NotificationEligibilityDecision.Eligible -> NotificationProcessingOutcome.PENDING
        is NotificationEligibilityDecision.Suppressed -> when (reason) {
            NotificationEligibilityDecision.Reason.INITIAL_BASELINE ->
                NotificationProcessingOutcome.BASELINED

            NotificationEligibilityDecision.Reason.STALE_SNAPSHOT ->
                NotificationProcessingOutcome.SUPPRESSED_ALREADY_PROCESSED

            NotificationEligibilityDecision.Reason.AMBIGUOUS_IDENTITY ->
                NotificationProcessingOutcome.AMBIGUOUS_IDENTITY

            NotificationEligibilityDecision.Reason.ALREADY_PROCESSED ->
                NotificationProcessingOutcome.SUPPRESSED_ALREADY_PROCESSED

            NotificationEligibilityDecision.Reason.OUTSIDE_WINDOW ->
                NotificationProcessingOutcome.SUPPRESSED_OUTSIDE_WINDOW

            NotificationEligibilityDecision.Reason.DISABLED ->
                NotificationProcessingOutcome.SUPPRESSED_DISABLED

            NotificationEligibilityDecision.Reason.PERMISSION_DENIED ->
                NotificationProcessingOutcome.SUPPRESSED_PERMISSION

            NotificationEligibilityDecision.Reason.MISSING_MAGNITUDE ->
                NotificationProcessingOutcome.SUPPRESSED_MISSING_MAGNITUDE

            NotificationEligibilityDecision.Reason.BELOW_THRESHOLD ->
                NotificationProcessingOutcome.SUPPRESSED_BELOW_THRESHOLD

            NotificationEligibilityDecision.Reason.AT_THRESHOLD ->
                NotificationProcessingOutcome.SUPPRESSED_AT_THRESHOLD
        }
    }

    private companion object {
        const val MAX_STATE_RETRIES = 3
        val EVENT_WINDOW: Duration = Duration.ofHours(24)
        val PROCESSING_HISTORY_RETENTION: Duration = Duration.ofDays(30)
        val PENDING_OUTCOMES = setOf(
            NotificationProcessingOutcome.PENDING.name,
            NotificationProcessingOutcome.RETRYABLE.name,
        )
    }
}
