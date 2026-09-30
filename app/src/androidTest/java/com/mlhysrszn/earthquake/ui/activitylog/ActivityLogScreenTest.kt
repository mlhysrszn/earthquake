package com.mlhysrszn.earthquake.ui.activitylog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mlhysrszn.earthquake.domain.model.NotificationDecision
import com.mlhysrszn.earthquake.domain.model.NotificationDecisionOutcome
import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.usecase.ProductMetrics
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityLogScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val time = Instant.parse("2026-09-30T12:00:00Z")

    @Test
    fun decisionsShowOutcomeInPlainTurkishAndEventsShowMetrics() {
        val events = listOf(
            event(ProductEventName.REFRESH_STARTED, "origin" to "foreground"),
            event(ProductEventName.REFRESH_FAILED, "origin" to "foreground", "reason" to "network"),
        )
        composeRule.setContent {
            ActivityLogScreen(
                state = ActivityLogUiState(
                    isLoading = false,
                    decisions = listOf(
                        decision("us1", 5.5, "Near Izmir", NotificationDecisionOutcome.POSTED),
                        decision("us2", 3.1, null, NotificationDecisionOutcome.SUPPRESSED_BELOW_THRESHOLD),
                    ),
                    events = events,
                    metrics = ProductMetrics.from(events),
                ),
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Bildirim gönderildi").assertIsDisplayed()
        composeRule.onNodeWithText("Near Izmir").assertIsDisplayed()
        composeRule.onNodeWithText("Eşiğin altında").assertIsDisplayed()
        composeRule.onNodeWithText("us2").assertIsDisplayed()

        composeRule.onNodeWithText("Olay kaydı").performClick()
        composeRule.onNodeWithText("Güncelleme hata oranı: 1/1 (%100)").assertIsDisplayed()
        composeRule.onNodeWithText("REFRESH_FAILED", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("reason=network", substring = true).assertIsDisplayed()
    }

    @Test
    fun emptyLogExplainsWhenDecisionsAppear() {
        composeRule.setContent {
            ActivityLogScreen(state = ActivityLogUiState(isLoading = false), onBack = {})
        }

        composeRule.onNodeWithText("Henüz karar yok. İlk güncellemeden sonra burada görünür.")
            .assertIsDisplayed()
    }

    private fun decision(
        id: String,
        magnitude: Double,
        place: String?,
        outcome: NotificationDecisionOutcome,
    ) = NotificationDecision(
        canonicalEventId = id,
        eventId = id,
        occurredAt = time,
        magnitude = magnitude,
        place = place,
        outcome = outcome,
        decidedAt = time,
    )

    private fun event(name: ProductEventName, vararg properties: Pair<String, String>) = ProductEvent(
        id = "${name.name}-${properties.toList()}",
        name = name,
        occurredAt = time,
        environment = ProductEventEnvironment.LIVE,
        properties = properties.toMap(),
    )
}
