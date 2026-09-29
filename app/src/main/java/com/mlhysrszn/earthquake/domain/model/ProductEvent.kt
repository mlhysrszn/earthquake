package com.mlhysrszn.earthquake.domain.model

import java.time.Instant

enum class ProductEventName {
    SCREEN_VIEW,
    NOTIFICATION_SETUP_STARTED,
    NOTIFICATION_SETUP_COMPLETED,
    NOTIFICATION_PREFERENCE_SAVED,
    PERMISSION_OUTCOME,
    NOTIFICATION_POSTED,
    NOTIFICATION_OPENED,
    NOTIFICATION_DELIVERY_SUPPRESSED,
    NOTIFICATION_DELIVERY_FAILED,
    REFRESH_STARTED,
    REFRESH_SUCCEEDED,
    REFRESH_FAILED,
    NOTIFICATION_PROCESSING_FAILED,
}

/** Identifies which source produced an event; Q02 demo data must never masquerade as live. */
enum class ProductEventEnvironment {
    LIVE,
    DEMO,
}

data class ProductEvent(
    val id: String,
    val name: ProductEventName,
    val occurredAt: Instant,
    val environment: ProductEventEnvironment,
    val properties: Map<String, String>,
)
