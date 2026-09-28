package com.mlhysrszn.earthquake.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.notifications.NotificationPermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deniedPermissionIsExplainedAndThresholdRemainsVisible() {
        composeRule.setContent {
            NotificationSettingsScreen(
                state = NotificationSettingsUiState(
                    preferences = NotificationPreferences(
                        notificationsEnabled = true,
                        magnitudeThreshold = 5.5,
                    ),
                ),
                permissionStatus = NotificationPermissionStatus.DENIED,
                onBack = {},
                onEnabledChange = {},
                onThresholdChange = {},
                onOpenSystemSettings = {},
            )
        }

        composeRule.onNodeWithText("Android bildirim izni verilmedi. İzin olmadan bildirim gösteremeyiz.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Büyüklük eşiği: 5,5").assertIsDisplayed()
        composeRule.onNodeWithText("Android bildirim ayarlarını aç").assertIsDisplayed()
    }

    @Test
    fun enablingCallbackIsInvokedAndThresholdSliderReportsHalfSteps() {
        var enabledValue: Boolean? = null
        var selectedThreshold: Double? = null
        composeRule.setContent {
            NotificationSettingsScreen(
                state = NotificationSettingsUiState(),
                permissionStatus = NotificationPermissionStatus.NOT_REQUIRED,
                onBack = {},
                onEnabledChange = { enabledValue = it },
                onThresholdChange = { selectedThreshold = it },
                onOpenSystemSettings = {},
            )
        }

        composeRule.onNodeWithTag(NOTIFICATION_SWITCH_TEST_TAG).performClick()
        composeRule.onNodeWithTag(MAGNITUDE_SLIDER_TEST_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(5.5f)
            }
        composeRule.runOnIdle {
            assertEquals(true, enabledValue)
            assertEquals(5.5, selectedThreshold!!, 0.0)
        }
    }

    @Test
    fun systemDisabledStatusOffersSettingsAction() {
        var openSettingsCount = 0
        composeRule.setContent {
            NotificationSettingsScreen(
                state = NotificationSettingsUiState(),
                permissionStatus = NotificationPermissionStatus.SYSTEM_DISABLED,
                onBack = {},
                onEnabledChange = {},
                onThresholdChange = {},
                onOpenSystemSettings = { openSettingsCount++ },
            )
        }

        composeRule.onNodeWithText("Bu uygulamanın bildirimleri Android ayarlarında kapalı.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Android bildirim ayarlarını aç").performClick()
        composeRule.runOnIdle { assertEquals(1, openSettingsCount) }
    }
}
