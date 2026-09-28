package com.mlhysrszn.earthquake.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mlhysrszn.earthquake.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EarthquakeNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun listItemOpensItsDetailAndBackReturnsToTheList() {
        composeRule.onNodeWithText("Central Alaska").performClick()

        composeRule.onNodeWithText("Deprem ayrıntıları").assertIsDisplayed()
        composeRule.onNodeWithText("Central Alaska").assertIsDisplayed()
        composeRule.onNodeWithText("Büyüklük 3,4").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Depremler").assertIsDisplayed()
        composeRule.onNodeWithText("Central Alaska").assertIsDisplayed()
    }

    @Test
    fun selectedEventIdAndBackStackSurviveActivityRecreation() {
        composeRule.onNodeWithText("Western Texas").performClick()
        composeRule.onNodeWithText("Büyüklük 2,1").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Deprem ayrıntıları").assertIsDisplayed()
        composeRule.onNodeWithText("Western Texas").assertIsDisplayed()
        composeRule.onNodeWithText("Büyüklük 2,1").assertIsDisplayed()
    }
}
