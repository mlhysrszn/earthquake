package com.mlhysrszn.earthquake

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.remote.usgs.UsgsEarthquakeService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Clock
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Provider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltGraphTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var clock: Clock
    @Inject lateinit var clockProvider: Provider<Clock>
    @Inject lateinit var usgsEarthquakeService: UsgsEarthquakeService
    @Inject lateinit var earthquakeDao: EarthquakeDao

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun graphProvidesSharedUtcClockAndActivityStartsOnEarthquakeList() {
        assertEquals(ZoneOffset.UTC, clock.zone)
        assertSame(clock, clockProvider.get())
        assertNotNull(usgsEarthquakeService)
        assertNotNull(earthquakeDao)
        val before = System.currentTimeMillis()
        val now = clock.millis()
        val after = System.currentTimeMillis()
        assertTrue(now in before..after)
        composeRule.onNodeWithText("Depremler").assertIsDisplayed()
    }
}
