package com.mlhysrszn.earthquake.ui.earthquakes.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentShowsMagnitudePlaceOccurrenceAndLastUpdate() {
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(
                    earthquakes = listOf(sample()),
                    isInitialLoading = false,
                    lastUpdatedAt = Instant.parse("2026-09-28T11:00:00Z"),
                    referenceTime = Instant.parse("2026-09-28T10:42:00Z"),
                ),
                onRefresh = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithContentDescription("Büyüklük 4,2").assertIsDisplayed()
        composeRule.onNodeWithText("Near the coast").assertIsDisplayed()
        composeRule.onNodeWithText("Oluşma zamanı:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("12 dk önce", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Son güncelleme:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Depremler").assertIsDisplayed()
    }

    @Test
    fun emptyStateProvidesRefreshAction() {
        var refreshCount = 0
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(isInitialLoading = false),
                onRefresh = { refreshCount += 1 },
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Son 24 saatte deprem bulunamadı.").assertIsDisplayed()
        composeRule.onNodeWithText("Yenile").performClick()
        composeRule.runOnIdle { assertEquals(1, refreshCount) }
    }

    @Test
    fun initialFailureOffersRetryAndExplainsTheProblem() {
        var retryCount = 0
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(
                    isInitialLoading = false,
                    refreshError = RefreshResult.Reason.NETWORK,
                ),
                onRefresh = {},
                onRetry = { retryCount += 1 },
            )
        }

        composeRule.onNodeWithText("Depremler yüklenemedi.").assertIsDisplayed()
        composeRule.onNodeWithText("Ağ bağlantısını kontrol edip tekrar deneyin.").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }

    @Test
    fun failedRefreshKeepsContentVisibleAndReportsAnError() {
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(
                    earthquakes = listOf(sample()),
                    isInitialLoading = false,
                    refreshError = RefreshResult.Reason.STORAGE,
                ),
                onRefresh = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Near the coast").assertIsDisplayed()
        composeRule.onNodeWithText("Güncelleme başarısız.").assertIsDisplayed()
        composeRule.onNodeWithText("Kayıtlı deprem verilerine erişilemedi. Lütfen tekrar deneyin.")
            .assertIsDisplayed()
    }

    @Test
    fun loadingStateShowsProgressLabel() {
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(),
                onRefresh = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Depremler yükleniyor").assertIsDisplayed()
    }

    @Test
    fun emptyListShowsProgressDuringManualRefresh() {
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(
                    isInitialLoading = false,
                    isRefreshing = true,
                ),
                onRefresh = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Depremler güncelleniyor").assertIsDisplayed()
    }

    @Test
    fun cachedContentRemainsVisibleDuringInitialRefresh() {
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(
                    earthquakes = listOf(sample()),
                    isInitialLoading = true,
                ),
                onRefresh = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Near the coast").assertIsDisplayed()
        composeRule.onNodeWithText("Depremler güncelleniyor").assertIsDisplayed()
    }

    @Test
    fun magnitudeFilterChipsReportSelection() {
        var selected: Double? = -1.0
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(earthquakes = listOf(sample()), isInitialLoading = false),
                onRefresh = {},
                onRetry = {},
                onMinimumMagnitudeChange = { selected = it },
            )
        }

        composeRule.onNodeWithText("4,0+").performClick()
        composeRule.runOnIdle { assertEquals(4.0, selected) }
        composeRule.onNodeWithText("Tümü").performClick()
        composeRule.runOnIdle { assertEquals(null, selected) }
    }

    @Test
    fun filterWithoutMatchesExplainsAndClears() {
        var selected: Double? = 5.0
        composeRule.setContent {
            EarthquakesScreen(
                state = EarthquakesUiState(
                    earthquakes = emptyList(),
                    totalEarthquakeCount = 3,
                    minimumMagnitude = 5.0,
                    isInitialLoading = false,
                ),
                onRefresh = {},
                onRetry = {},
                onMinimumMagnitudeChange = { selected = it },
            )
        }

        composeRule.onNodeWithText("Bu filtreyle eşleşen deprem yok.").assertIsDisplayed()
        composeRule.onNodeWithText("Filtreyi kaldır").performClick()
        composeRule.runOnIdle { assertEquals(null, selected) }
    }

    private fun sample() = Earthquake(
        id = "test-event-1",
        magnitude = 4.2,
        magnitudeType = "ml",
        place = "Near the coast",
        occurredAt = Instant.parse("2026-09-28T10:30:00Z"),
        updatedAt = null,
        longitude = -122.0,
        latitude = 37.0,
        depthKm = 8.0,
        sourceUrl = null,
    )
}
