package com.mlhysrszn.earthquake.domain.model

data class NotificationPreferences(
    val notificationsEnabled: Boolean = DEFAULT_NOTIFICATIONS_ENABLED,
    val magnitudeThreshold: Double = DEFAULT_MAGNITUDE_THRESHOLD,
) {
    init {
        require(isValidMagnitudeThreshold(magnitudeThreshold)) {
            "Magnitude threshold must be between $MIN_MAGNITUDE_THRESHOLD and " +
                "$MAX_MAGNITUDE_THRESHOLD in steps of $MAGNITUDE_THRESHOLD_STEP"
        }
    }

    companion object {
        const val DEFAULT_NOTIFICATIONS_ENABLED = false
        const val DEFAULT_MAGNITUDE_THRESHOLD = 4.0
        const val MIN_MAGNITUDE_THRESHOLD = 0.0
        const val MAX_MAGNITUDE_THRESHOLD = 9.5
        const val MAGNITUDE_THRESHOLD_STEP = 0.5
    }
}

fun isValidMagnitudeThreshold(value: Double): Boolean {
    if (
        !value.isFinite() ||
        value < NotificationPreferences.MIN_MAGNITUDE_THRESHOLD ||
        value > NotificationPreferences.MAX_MAGNITUDE_THRESHOLD
    ) {
        return false
    }
    return (value / NotificationPreferences.MAGNITUDE_THRESHOLD_STEP) % 1.0 == 0.0
}
