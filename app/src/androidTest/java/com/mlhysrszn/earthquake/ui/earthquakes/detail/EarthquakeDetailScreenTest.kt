package com.mlhysrszn.earthquake.ui.earthquakes.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakeDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unavailableEventHasExplicitMessageAndRetryAction() {
        var retryCount = 0
        var backCount = 0
        composeRule.setContent {
            EarthquakeDetailScreen(
                state = EarthquakeDetailUiState.Unavailable,
                onBack = { backCount++ },
                onRetry = { retryCount++ },
            )
        }

        composeRule.onNodeWithText("Bu deprem bulunamadı veya artık mevcut değil.").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        composeRule.onNodeWithText("Geri").performClick()
        composeRule.runOnIdle {
            assertEquals(1, retryCount)
            assertEquals(1, backCount)
        }
    }

    @Test
    fun detailFailureExplainsTheProblemAndOffersRetry() {
        var retryCount = 0
        composeRule.setContent {
            EarthquakeDetailScreen(
                state = EarthquakeDetailUiState.Failure(RefreshResult.Reason.NETWORK),
                onBack = {},
                onRetry = { retryCount++ },
            )
        }

        composeRule.onNodeWithText("Deprem ayrıntıları yüklenemedi.").assertIsDisplayed()
        composeRule.onNodeWithText("Ağ bağlantısını kontrol edip tekrar deneyin.").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }
}
