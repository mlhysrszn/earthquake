package com.mlhysrszn.earthquake.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Reconciles the persisted user preference with unique periodic background work. */
@Singleton
class EarthquakeSyncWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun setNotificationsEnabled(enabled: Boolean): Operation {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            return workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        return workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            createPeriodicRequest(),
        )
    }

    internal fun createPeriodicRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<EarthquakeSyncWorker>(
            PERIOD_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .build()

    companion object {
        const val UNIQUE_WORK_NAME = "earthquake-periodic-sync"
        const val PERIOD_MINUTES = 15L
        const val INITIAL_BACKOFF_SECONDS = 30L
    }
}
