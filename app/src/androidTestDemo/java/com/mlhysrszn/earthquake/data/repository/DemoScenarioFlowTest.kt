package com.mlhysrszn.earthquake.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mlhysrszn.earthquake.data.local.demo.DemoScenarioDatabase
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase
import com.mlhysrszn.earthquake.data.notification.NotificationProcessor
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.usecase.NotificationEligibilityPolicy
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoScenarioFlowTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC)
    private lateinit var database: EarthquakeDatabase
    private lateinit var scenarioDatabase: DemoScenarioDatabase
    private lateinit var controller: RoomDemoScenarioController
    private lateinit var repository: DemoEarthquakeRepository
    private lateinit var processor: NotificationProcessor

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, EarthquakeDatabase::class.java).build()
        scenarioDatabase = Room.inMemoryDatabaseBuilder(context, DemoScenarioDatabase::class.java).build()
        val scenarioDao = scenarioDatabase.demoScenarioDao()
        controller = RoomDemoScenarioController(scenarioDao, scenarioDatabase, database, clock)
        repository = DemoEarthquakeRepository(database.earthquakeDao(), scenarioDao, clock)
        processor = NotificationProcessor(
            earthquakeDao = database.earthquakeDao(),
            processingDao = database.notificationProcessingDao(),
            preferencesRepository = EnabledPreferences,
            eligibilityPolicy = NotificationEligibilityPolicy(clock),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        scenarioDatabase.close()
        database.close()
    }

    @Test
    fun belowThresholdAboveThresholdAndDuplicateFollowRealRules() = runBlocking {
        refreshAndProcess() // empty baseline

        controller.addBelowThreshold()
        assertTrue(refreshAndProcess().isEmpty())

        controller.addAboveThreshold()
        val aboveIds = refreshAndProcess()
        assertEquals(1, aboveIds.size)
        assertTrue(processor.markPosted(aboveIds.single()))

        assertTrue(controller.replayLatestAsDuplicate())
        assertTrue(refreshAndProcess().isEmpty())
        assertEquals(2, repository.observeEarthquakes().first().size)
    }

    @Test
    fun resetClearsScenarioAndCanBeRepeated() = runBlocking {
        refreshAndProcess()
        controller.addAboveThreshold()
        assertEquals(1, refreshAndProcess().size)

        controller.reset()
        refreshAndProcess()
        assertEquals(0, controller.observeStatus().first().eventCount)
        assertTrue(repository.observeEarthquakes().first().isEmpty())

        controller.addAboveThreshold()
        assertEquals(1, refreshAndProcess().size)
    }

    private suspend fun refreshAndProcess(): List<String> {
        assertTrue(repository.refresh() is RefreshResult.Success)
        return processor.processLatestSnapshot(notificationPermissionGranted = true)
            .pendingNotifications.map { it.canonicalEventId }
    }

    private object EnabledPreferences : NotificationPreferencesRepository {
        override fun observePreferences(): Flow<NotificationPreferences> =
            flowOf(NotificationPreferences(notificationsEnabled = true, magnitudeThreshold = 4.0))

        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit

        override suspend fun setMagnitudeThreshold(threshold: Double) = Unit
    }
}
