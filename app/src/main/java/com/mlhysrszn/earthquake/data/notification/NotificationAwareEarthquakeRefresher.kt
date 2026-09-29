package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import com.mlhysrszn.earthquake.domain.usecase.DispatchPendingNotifications
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Shared foreground/background refresh path; failed snapshots never reach notification processing. */
@Singleton
class NotificationAwareEarthquakeRefresher @Inject constructor(
    private val earthquakeRepository: EarthquakeRepository,
    private val dispatchPendingNotifications: DispatchPendingNotifications,
    private val productEventRecorder: ProductEventRecorder,
) : RefreshEarthquakes {
    override suspend fun refresh(origin: RefreshOrigin): RefreshResult {
        val originProperty = origin.name.lowercase()
        productEventRecorder.record(
            name = ProductEventName.REFRESH_STARTED,
            properties = mapOf("origin" to originProperty),
        )

        val result = try {
            earthquakeRepository.refresh()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
        }

        when (result) {
            is RefreshResult.Failure -> {
                productEventRecorder.record(
                    name = ProductEventName.REFRESH_FAILED,
                    properties = mapOf(
                        "origin" to originProperty,
                        "reason" to result.reason.name.lowercase(),
                    ),
                )
                return result
            }

            is RefreshResult.Success -> {
                productEventRecorder.record(
                    name = ProductEventName.REFRESH_SUCCEEDED,
                    properties = mapOf(
                        "origin" to originProperty,
                        "accepted_count" to result.acceptedEventCount.toString(),
                    ),
                )
            }
        }

        return try {
            dispatchPendingNotifications.dispatch()
            result
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            productEventRecorder.record(
                name = ProductEventName.NOTIFICATION_PROCESSING_FAILED,
                properties = mapOf("origin" to originProperty),
            )
            RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
        }
    }
}
