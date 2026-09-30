package com.mlhysrszn.earthquake.domain.usecase

import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.model.ProductEventName

/** A count-based rate; [percent] is null when nothing was measured yet. */
data class Ratio(val numerator: Int, val denominator: Int) {
    val percent: Int?
        get() = if (denominator == 0) null else numerator * 100 / denominator
}

/** The three success metrics defined in docs/PRODUCT_EVENTS.md, computed from local events. */
data class ProductMetrics(
    val setupCompletion: Ratio,
    val notificationOpening: Ratio,
    val refreshFailure: Ratio,
) {
    companion object {
        fun from(events: List<ProductEvent>): ProductMetrics {
            fun ids(name: ProductEventName, key: String): Set<String> = events
                .filter { it.name == name }
                .mapNotNull { it.properties[key] }
                .toSet()

            fun count(name: ProductEventName): Int = events.count { it.name == name }

            val startedAttempts = ids(ProductEventName.NOTIFICATION_SETUP_STARTED, ATTEMPT_ID)
            val completedAttempts = ids(ProductEventName.NOTIFICATION_SETUP_COMPLETED, ATTEMPT_ID)
            val postedEvents = ids(ProductEventName.NOTIFICATION_POSTED, EVENT_ID)
            val openedEvents = ids(ProductEventName.NOTIFICATION_OPENED, EVENT_ID)
            return ProductMetrics(
                setupCompletion = Ratio(
                    numerator = (completedAttempts intersect startedAttempts).size,
                    denominator = startedAttempts.size,
                ),
                notificationOpening = Ratio(
                    numerator = (openedEvents intersect postedEvents).size,
                    denominator = postedEvents.size,
                ),
                refreshFailure = Ratio(
                    numerator = count(ProductEventName.REFRESH_FAILED),
                    denominator = count(ProductEventName.REFRESH_STARTED),
                ),
            )
        }

        private const val ATTEMPT_ID = "attempt_id"
        private const val EVENT_ID = "event_id"
    }
}
