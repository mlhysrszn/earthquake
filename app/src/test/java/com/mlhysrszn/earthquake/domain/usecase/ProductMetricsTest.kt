package com.mlhysrszn.earthquake.domain.usecase

import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductMetricsTest {
    @Test
    fun `no events means no measured rates`() {
        val metrics = ProductMetrics.from(emptyList())

        assertEquals(Ratio(0, 0), metrics.setupCompletion)
        assertNull(metrics.setupCompletion.percent)
    }

    @Test
    fun `setup completion counts distinct started attempts that completed`() {
        val metrics = ProductMetrics.from(
            listOf(
                event(ProductEventName.NOTIFICATION_SETUP_STARTED, "attempt_id" to "a"),
                event(ProductEventName.NOTIFICATION_SETUP_STARTED, "attempt_id" to "b"),
                event(ProductEventName.NOTIFICATION_SETUP_COMPLETED, "attempt_id" to "a"),
                event(ProductEventName.NOTIFICATION_SETUP_COMPLETED, "attempt_id" to "a"),
            ),
        )

        assertEquals(Ratio(1, 2), metrics.setupCompletion)
        assertEquals(50, metrics.setupCompletion.percent)
    }

    @Test
    fun `opening counts only opened events that were posted`() {
        val metrics = ProductMetrics.from(
            listOf(
                event(ProductEventName.NOTIFICATION_POSTED, "event_id" to "us1"),
                event(ProductEventName.NOTIFICATION_POSTED, "event_id" to "us2"),
                event(ProductEventName.NOTIFICATION_OPENED, "event_id" to "us1"),
                event(ProductEventName.NOTIFICATION_OPENED, "event_id" to "external"),
            ),
        )

        assertEquals(Ratio(1, 2), metrics.notificationOpening)
    }

    @Test
    fun `refresh failure rate divides failures by started refreshes`() {
        val metrics = ProductMetrics.from(
            List(4) { event(ProductEventName.REFRESH_STARTED) } +
                event(ProductEventName.REFRESH_FAILED),
        )

        assertEquals(Ratio(1, 4), metrics.refreshFailure)
        assertEquals(25, metrics.refreshFailure.percent)
    }

    private fun event(name: ProductEventName, vararg properties: Pair<String, String>) = ProductEvent(
        id = "${name.name}-${properties.toList()}-${counter++}",
        name = name,
        occurredAt = Instant.parse("2026-09-30T12:00:00Z"),
        environment = ProductEventEnvironment.LIVE,
        properties = properties.toMap(),
    )

    private var counter = 0
}
