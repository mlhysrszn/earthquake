package com.mlhysrszn.earthquake.background

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class EarthquakeSyncWorkSchedulerTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Inject lateinit var scheduler: EarthquakeSyncWorkScheduler

    @Before
    fun initializeWorkManager() {
        hiltRule.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        WorkManager.getInstance(context).cancelAllWork().result.get(10, TimeUnit.SECONDS)
    }

    @After
    fun closeWorkManager() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun enabledPreferencesEnqueueOneUniquePeriodicRequestAndDisabledCancelsIt() {
        val workManager = WorkManager.getInstance(context)

        scheduler.setNotificationsEnabled(true).result.get(10, TimeUnit.SECONDS)
        val first = workManager.getWorkInfosForUniqueWork(
            scheduler.uniqueWorkName,
        ).get(10, TimeUnit.SECONDS).single()

        scheduler.setNotificationsEnabled(true).result.get(10, TimeUnit.SECONDS)
        val afterDuplicateSchedule = workManager.getWorkInfosForUniqueWork(
            scheduler.uniqueWorkName,
        ).get(10, TimeUnit.SECONDS).single()
        assertEquals(first.id, afterDuplicateSchedule.id)
        assertEquals(WorkInfo.State.ENQUEUED, afterDuplicateSchedule.state)

        scheduler.setNotificationsEnabled(false).result.get(10, TimeUnit.SECONDS)
        val afterCancel = workManager.getWorkInfosForUniqueWork(
            scheduler.uniqueWorkName,
        ).get(10, TimeUnit.SECONDS).single()
        assertEquals(WorkInfo.State.CANCELLED, afterCancel.state)
    }

    @Test
    fun periodicRequestRequiresNetworkAndUsesSystemMinimumInterval() {
        val request = scheduler.createPeriodicRequest()

        assertTrue(request.workSpec.constraints.requiredNetworkType == androidx.work.NetworkType.CONNECTED)
        assertEquals(
            TimeUnit.MINUTES.toMillis(EarthquakeSyncWorkScheduler.PERIOD_MINUTES),
            request.workSpec.intervalDuration,
        )
    }
}
