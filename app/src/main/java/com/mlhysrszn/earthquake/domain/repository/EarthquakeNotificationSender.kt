package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.Earthquake

data class EarthquakeNotificationRequest(
    val canonicalEventId: String,
    val earthquake: Earthquake,
)

sealed interface NotificationDeliveryResult {
    data object Posted : NotificationDeliveryResult

    data object PermissionDenied : NotificationDeliveryResult

    data object SystemDisabled : NotificationDeliveryResult

    data object RetryableFailure : NotificationDeliveryResult
}

interface EarthquakeNotificationSender {
    suspend fun send(request: EarthquakeNotificationRequest): NotificationDeliveryResult
}
