package com.mlhysrszn.earthquake.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mlhysrszn.earthquake.MainActivity
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.repository.ProductEventRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    @Inject lateinit var productEventRepository: ProductEventRepository

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

    @Test
    fun listViewIsNotDuplicatedByRefreshRecomposition() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Yenile").assertIsEnabled()
        val listViewsBeforeRefresh = runBlocking { listViewCount() }

        composeRule.onNodeWithText("Yenile").performClick()
        composeRule.waitForIdle()

        val listViewsAfterRefresh = runBlocking { listViewCount() }
        assertEquals(listViewsBeforeRefresh, listViewsAfterRefresh)
    }

    private suspend fun listViewCount(): Int =
        productEventRepository.getRecentEvents(500).count { event ->
            event.name == ProductEventName.SCREEN_VIEW &&
                event.properties["screen"] == "list"
        }

    @Test
    fun listOpensNotificationSettingsAndShowsThisDevicesPermissionState() {
        composeRule.onNodeWithText("Bildirimler").performClick()

        composeRule.onNodeWithText("Bildirim ayarları").assertIsDisplayed()
        composeRule.onNodeWithText("Bu Android sürümünde ayrıca bildirim izni gerekmiyor.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Geri").performClick()
        composeRule.onNodeWithText("Depremler").assertIsDisplayed()
    }

    @Test
    fun settingsOpensTheActivityLogWithDecisionsEventsAndMetrics() {
        composeRule.onNodeWithText("Bildirimler").performClick()
        composeRule.onNodeWithText("Kayıtları görüntüle").performScrollTo().performClick()

        composeRule.onNodeWithText("Kayıtlar").assertIsDisplayed()
        composeRule.onNodeWithText("Bildirim kararları").assertIsDisplayed()

        composeRule.onNodeWithText("Olay kaydı").performClick()
        composeRule.onNodeWithText("Ürün metrikleri").assertIsDisplayed()

        composeRule.onNodeWithText("Geri").performClick()
        composeRule.onNodeWithText("Bildirim ayarları").assertIsDisplayed()
    }
}
