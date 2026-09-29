package com.mlhysrszn.earthquake

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mlhysrszn.earthquake.background.EarthquakeSyncWorkScheduler
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class EarthquakeApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var preferencesRepository: NotificationPreferencesRepository
    @Inject lateinit var workScheduler: EarthquakeSyncWorkScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                preferencesRepository.observePreferences()
                    .map { preferences -> preferences.notificationsEnabled }
                    .distinctUntilChanged()
                    .collect { enabled -> workScheduler.setNotificationsEnabled(enabled) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Log.e(TAG, "Could not synchronize periodic earthquake work", exception)
            }
        }
    }

    private companion object {
        const val TAG = "EarthquakeApplication"
    }
}
