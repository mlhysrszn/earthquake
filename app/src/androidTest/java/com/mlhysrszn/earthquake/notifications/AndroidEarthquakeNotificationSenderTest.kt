package com.mlhysrszn.earthquake.notifications

import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.mlhysrszn.earthquake.MainActivity
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeNotificationRequest
import com.mlhysrszn.earthquake.domain.repository.NotificationDeliveryResult
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AndroidEarthquakeNotificationSenderTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var sender: AndroidEarthquakeNotificationSender

    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context
        get() = instrumentation.targetContext

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun postsStableNotificationAndColdPendingIntentOpensTheCorrectDetail() = runBlocking {
        val earthquake = sample()
        val canonicalEventId = "canonical-${earthquake.id}"
        val result = sender.send(
            EarthquakeNotificationRequest(
                canonicalEventId = canonicalEventId,
                earthquake = earthquake,
            ),
        )

        assertEquals(NotificationDeliveryResult.Posted, result)
        val notificationManager = NotificationManagerCompat.from(context)
        val activeNotification = notificationManager.activeNotifications
            .firstOrNull { it.tag == canonicalEventId }
        assertNotNull(activeNotification)
        assertEquals(EarthquakeNotificationConstants.NOTIFICATION_ID, activeNotification!!.id)

        val device = UiDevice.getInstance(instrumentation)
        device.pressHome()
        val activityMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            activeNotification.notification.contentIntent.send()
            val openedActivity = instrumentation.waitForMonitorWithTimeout(activityMonitor, 10_000)
                as? MainActivity

            assertNotNull(openedActivity)
            assertEquals(
                earthquake.id,
                openedActivity!!.intent.getStringExtra(MainActivity.EXTRA_EARTHQUAKE_ID),
            )
            assertTrue(device.wait(Until.hasObject(By.text("Deprem ayrıntıları")), 10_000))
            assertTrue(device.wait(Until.hasObject(By.text("Central Alaska")), 10_000))
        } finally {
            instrumentation.removeMonitor(activityMonitor)
            notificationManager.cancel(canonicalEventId, EarthquakeNotificationConstants.NOTIFICATION_ID)
        }
    }

    private fun sample() = Earthquake(
        id = "sample-002",
        magnitude = 3.4,
        magnitudeType = "ml",
        place = "Central Alaska",
        occurredAt = Instant.now().minusSeconds(60),
        updatedAt = null,
        longitude = -150.1,
        latitude = 63.2,
        depthKm = 35.0,
        sourceUrl = null,
    )
}
