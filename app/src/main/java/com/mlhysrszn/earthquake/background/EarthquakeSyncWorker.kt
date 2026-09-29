package com.mlhysrszn.earthquake.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class EarthquakeSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshEarthquakes: RefreshEarthquakes,
    private val preferencesRepository: NotificationPreferencesRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val enabled = try {
            preferencesRepository.observePreferences().first().notificationsEnabled
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return retryOrFail()
        }

        if (!enabled) return Result.success()

        return when (val result = try {
            refreshEarthquakes.refresh(RefreshOrigin.BACKGROUND)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
        }) {
            is RefreshResult.Success -> Result.success()
            is RefreshResult.Failure -> when (result.reason) {
                RefreshResult.Reason.INVALID_RESPONSE -> Result.failure()
                RefreshResult.Reason.NETWORK,
                RefreshResult.Reason.STORAGE,
                RefreshResult.Reason.UNKNOWN,
                -> retryOrFail()
            }
        }
    }

    /** Failure ends this bounded attempt cycle; periodic work remains scheduled for its next period. */
    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_RUN_ATTEMPTS - 1) Result.retry() else Result.failure()

    private companion object {
        const val MAX_RUN_ATTEMPTS = 5
    }
}
