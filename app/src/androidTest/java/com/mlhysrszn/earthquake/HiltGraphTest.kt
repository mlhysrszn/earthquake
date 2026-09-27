package com.mlhysrszn.earthquake

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Clock
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Provider
import org.junit.Assert.assertEquals
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

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun graphProvidesSharedUtcClockAndActivityStartsOnEarthquakeList() {
        assertEquals(ZoneOffset.UTC, clock.zone)
        assertSame(clock, clockProvider.get())
        val before = System.currentTimeMillis()
        val now = clock.millis()
        val after = System.currentTimeMillis()
        assertTrue(now in before..after)
        composeRule.onNodeWithText("Depremler").assertIsDisplayed()
    }
}
