package com.mlhysrszn.earthquake.notifications

import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.mlhysrszn.earthquake.MainActivity
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingOutcome
import com.mlhysrszn.earthquake.data.local.room.toEntity
import com.mlhysrszn.earthquake.data.notification.NotificationProcessor
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin
import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationSender
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import com.mlhysrszn.earthquake.domain.repository.ProductEventRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.usecase.DispatchPendingNotifications
import com.mlhysrszn.earthquake.domain.usecase.NotificationEligibilityPolicy
import com.mlhysrszn.earthquake.data.notification.NotificationAwareEarthquakeRefresher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NotificationDispatchIntegrationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var sender: EarthquakeNotificationSender
    @Inject lateinit var productEventRepository: ProductEventRepository

    @Before
    fun inject() {
        hiltRule.inject()
    }

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val nowMillis = now.toEpochMilli()
    private val sourceCursor = nowMillis - 60_000
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun eligibleEventPostsNotificationAndColdTapOpensItsDetail() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, EarthquakeDatabase::class.java).build()
        val earthquake = sample()
        val canonicalId = earthquake.id
        val notificationManager = NotificationManagerCompat.from(context)
        var launchedActivity: MainActivity? = null
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activityMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)

        try {
            val earthquakeDao = database.earthquakeDao()
            val processingDao = database.notificationProcessingDao()
            val processor = NotificationProcessor(
                earthquakeDao = earthquakeDao,
                processingDao = processingDao,
                preferencesRepository = FakeNotificationPreferencesRepository(),
                eligibilityPolicy = NotificationEligibilityPolicy(clock),
                clock = clock,
            )
            storeSnapshot(earthquakeDao, emptyList(), sourceCursor)
            assertTrue(processor.processLatestSnapshot(notificationPermissionGranted = true)
                .pendingNotifications.isEmpty())
            storeSnapshot(earthquakeDao, listOf(earthquake), sourceCursor + 30_000)

            val dispatcher = PendingNotificationDispatcher(
                context = context,
                processor = processor,
                earthquakeRepository = FakeEarthquakeRepository(earthquake),
                sender = sender,
            )
            val sharedRefresher = NotificationAwareEarthquakeRefresher(
                earthquakeRepository = FakeEarthquakeRepository(earthquake),
                dispatchPendingNotifications = DispatchPendingNotifications { dispatcher.dispatch() },
                productEventRecorder = ProductEventRecorder { _, _, _ -> },
            )
            assertEquals(
                RefreshResult.Success(acceptedEventCount = 1),
                sharedRefresher.refresh(RefreshOrigin.FOREGROUND),
            )
            assertEquals(
                RefreshResult.Success(acceptedEventCount = 1),
                sharedRefresher.refresh(RefreshOrigin.FOREGROUND),
            )
            assertTrue(
                productEventRepository.getRecentEvents().any { event ->
                    event.name == ProductEventName.NOTIFICATION_POSTED &&
                        event.properties["event_id"] == earthquake.id
                },
            )

            val activeNotification = notificationManager.activeNotifications
                .firstOrNull { it.tag == canonicalId }
            assertNotNull(activeNotification)
            assertEquals(
                NotificationProcessingOutcome.POSTED.name,
                processingDao.getEventState(canonicalId)?.outcome,
            )

            val device = UiDevice.getInstance(instrumentation)
            device.pressHome()
            activeNotification!!.notification.contentIntent.send()
            launchedActivity = instrumentation.waitForMonitorWithTimeout(activityMonitor, 10_000)
                as? MainActivity

            assertNotNull(launchedActivity)
            assertEquals(
                earthquake.id,
                launchedActivity!!.intent.getStringExtra(MainActivity.EXTRA_EARTHQUAKE_ID),
            )
            assertTrue(device.wait(Until.hasObject(By.text("Deprem ayrıntıları")), 10_000))
            assertTrue(device.wait(Until.hasObject(By.text(earthquake.place!!)), 10_000))
            withTimeout(10_000) {
                productEventRepository.observeRecentEvents().first { events ->
                    events.any { event ->
                        event.name == ProductEventName.NOTIFICATION_OPENED &&
                            event.properties["event_id"] == earthquake.id
                    } && events.any { event ->
                        event.name == ProductEventName.SCREEN_VIEW &&
                            event.properties["screen"] == "detail" &&
                            event.properties["entry_source"] == "notification" &&
                            event.properties["event_id"] == earthquake.id
                    }
                }
            }
        } finally {
            launchedActivity?.finish()
            instrumentation.removeMonitor(activityMonitor)
            notificationManager.cancel(canonicalId, EarthquakeNotificationConstants.NOTIFICATION_ID)
            database.close()
        }
        Unit
    }

    private suspend fun storeSnapshot(dao: EarthquakeDao, events: List<Earthquake>, generatedAt: Long) {
        dao.applySnapshot(
            earthquakes = events.map(Earthquake::toEntity),
            sourceGeneratedAtEpochMillis = generatedAt,
            fetchedAtEpochMillis = nowMillis,
            nowEpochMillis = nowMillis,
        )
    }

    private fun sample() = Earthquake(
        id = "sample-001",
        magnitude = 5.1,
        magnitudeType = "mw",
        place = "Near the coast of Northern California",
        occurredAt = now.minusSeconds(30),
        updatedAt = null,
        longitude = -124.2,
        latitude = 40.3,
        depthKm = 12.0,
        sourceUrl = null,
    )

    private class FakeNotificationPreferencesRepository : NotificationPreferencesRepository {
        private val preferences = MutableStateFlow(
            NotificationPreferences(notificationsEnabled = true, magnitudeThreshold = 4.0),
        )

        override fun observePreferences(): Flow<NotificationPreferences> = preferences

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            preferences.value = preferences.value.copy(notificationsEnabled = enabled)
        }

        override suspend fun setMagnitudeThreshold(threshold: Double) {
            preferences.value = preferences.value.copy(magnitudeThreshold = threshold)
        }
    }

    private class FakeEarthquakeRepository(
        private val earthquake: Earthquake,
    ) : EarthquakeRepository {
        override fun observeEarthquakes(): Flow<List<Earthquake>> =
            MutableStateFlow(listOf(earthquake))

        override fun observeEarthquake(id: String): Flow<Earthquake?> =
            MutableStateFlow(earthquake.takeIf { it.id == id })

        override suspend fun refresh() = RefreshResult.Success(1)

        override suspend fun fetchEarthquakeById(id: String): EarthquakeLookupResult =
            if (id == earthquake.id) EarthquakeLookupResult.Found(earthquake)
            else EarthquakeLookupResult.Unavailable
    }
}
