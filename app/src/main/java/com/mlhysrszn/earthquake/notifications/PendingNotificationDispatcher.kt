package com.mlhysrszn.earthquake.notifications

import android.content.Context
import com.mlhysrszn.earthquake.data.notification.NotificationProcessor
import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationRequest
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationSender
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.NotificationDeliveryResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Connects persisted eligibility decisions to the permission-aware Android sender. */
@Singleton
class PendingNotificationDispatcher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val processor: NotificationProcessor,
    private val earthquakeRepository: EarthquakeRepository,
    private val sender: EarthquakeNotificationSender,
) {
    suspend fun dispatchPending(): Int {
        val permissionStatus = notificationPermissionStatus(context)
        val permissionGranted = permissionStatus == NotificationPermissionStatus.GRANTED ||
            permissionStatus == NotificationPermissionStatus.NOT_REQUIRED
        val batch = processor.processLatestSnapshot(
            notificationPermissionGranted = permissionGranted,
        )
        var postedCount = 0

        for (candidate in batch.pendingNotifications) {
            val earthquake = earthquakeRepository.observeEarthquake(candidate.eventId).first()
                ?: when (val lookup = earthquakeRepository.fetchEarthquakeById(candidate.eventId)) {
                    is EarthquakeLookupResult.Found -> lookup.earthquake
                    EarthquakeLookupResult.Unavailable -> {
                        processor.markExpired(candidate.canonicalEventId)
                        continue
                    }

                    is EarthquakeLookupResult.Failure -> {
                        processor.markRetryable(candidate.canonicalEventId)
                        continue
                    }
                }

            when (
                sender.send(
                    EarthquakeNotificationRequest(
                        canonicalEventId = candidate.canonicalEventId,
                        earthquake = earthquake,
                    ),
                )
            ) {
                NotificationDeliveryResult.Posted -> {
                    if (processor.markPosted(candidate.canonicalEventId)) postedCount++
                }

                NotificationDeliveryResult.PermissionDenied,
                NotificationDeliveryResult.SystemDisabled,
                -> processor.markPermissionDenied(candidate.canonicalEventId)

                NotificationDeliveryResult.RetryableFailure ->
                    processor.markRetryable(candidate.canonicalEventId)
            }
        }
        return postedCount
    }
}
