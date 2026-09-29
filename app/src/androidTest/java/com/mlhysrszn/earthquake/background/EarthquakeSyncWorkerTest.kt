package com.mlhysrszn.earthquake.background

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EarthquakeSyncWorkerTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory
    @Inject lateinit var appPreferencesRepository: NotificationPreferencesRepository

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun generatedHiltWorkerFactoryCreatesWorkerWithoutAnActivityScope() = runBlocking {
        appPreferencesRepository.setNotificationsEnabled(false)
        val worker = TestListenableWorkerBuilder
            .from(
                InstrumentationRegistry.getInstrumentation().targetContext,
                EarthquakeSyncWorker::class.java,
            )
            .setWorkerFactory(hiltWorkerFactory)
            .build()

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun disabledNotificationsSkipRefreshAndSucceed() = runBlocking {
        var refreshCount = 0
        val worker = createWorker(
            enabled = false,
            result = RefreshResult.Success(acceptedEventCount = 0),
            onRefresh = { refreshCount++ },
        )

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(0, refreshCount)
    }

    @Test
    fun successfulSyncCompletesSuccessfully() = runBlocking {
        var refreshCount = 0
        val worker = createWorker(
            enabled = true,
            result = RefreshResult.Success(acceptedEventCount = 3),
            onRefresh = { refreshCount++ },
        )

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(1, refreshCount)
    }

    @Test
    fun eachPeriodicExecutionReentersTheSharedRefreshAndNotificationPath() = runBlocking {
        var refreshCount = 0
        val firstExecution = createWorker(
            enabled = true,
            result = RefreshResult.Success(acceptedEventCount = 0),
            onRefresh = { refreshCount++ },
        )
        val nextExecution = createWorker(
            enabled = true,
            result = RefreshResult.Success(acceptedEventCount = 0),
            onRefresh = { refreshCount++ },
        )

        assertEquals(ListenableWorker.Result.success(), firstExecution.doWork())
        assertEquals(ListenableWorker.Result.success(), nextExecution.doWork())
        assertEquals(2, refreshCount)
    }

    @Test
    fun transientFailureRetriesThenStopsAtTheConfiguredAttemptLimit() = runBlocking {
        val failure = RefreshResult.Failure(RefreshResult.Reason.NETWORK)
        val retryWorker = createWorker(enabled = true, result = failure, runAttemptCount = 0)
        val finalWorker = createWorker(enabled = true, result = failure, runAttemptCount = 4)

        assertEquals(ListenableWorker.Result.retry(), retryWorker.doWork())
        assertEquals(ListenableWorker.Result.failure(), finalWorker.doWork())
    }

    @Test
    fun invalidResponseFailsImmediatelyWithoutRetry() = runBlocking {
        val worker = createWorker(
            enabled = true,
            result = RefreshResult.Failure(RefreshResult.Reason.INVALID_RESPONSE),
        )

        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }

    private fun createWorker(
        enabled: Boolean,
        result: RefreshResult,
        runAttemptCount: Int = 0,
        onRefresh: () -> Unit = {},
    ): EarthquakeSyncWorker {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = FakePreferencesRepository(enabled)
        val refresher = RefreshEarthquakes {
            onRefresh()
            result
        }
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? = EarthquakeSyncWorker(
                appContext,
                workerParameters,
                refresher,
                preferences,
            )
        }

        return TestListenableWorkerBuilder
            .from(context, EarthquakeSyncWorker::class.java)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(factory)
            .build()
    }

    private class FakePreferencesRepository(enabled: Boolean) : NotificationPreferencesRepository {
        private val preferences = MutableStateFlow(
            NotificationPreferences(notificationsEnabled = enabled),
        )

        override fun observePreferences(): Flow<NotificationPreferences> = preferences

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            preferences.value = preferences.value.copy(notificationsEnabled = enabled)
        }

        override suspend fun setMagnitudeThreshold(threshold: Double) {
            preferences.value = preferences.value.copy(magnitudeThreshold = threshold)
        }
    }
}
