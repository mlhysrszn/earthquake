package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
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
) : RefreshEarthquakes {
    override suspend fun refresh(): RefreshResult = try {
        when (val result = earthquakeRepository.refresh()) {
            is RefreshResult.Success -> {
                dispatchPendingNotifications.dispatch()
                result
            }

            is RefreshResult.Failure -> result
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
    }
}
