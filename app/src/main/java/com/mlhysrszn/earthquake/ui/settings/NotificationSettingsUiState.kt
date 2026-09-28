package com.mlhysrszn.earthquake.ui.settings

import com.mlhysrszn.earthquake.domain.model.NotificationPreferences

data class NotificationSettingsUiState(
    val preferences: NotificationPreferences = NotificationPreferences(),
    val saveFailed: Boolean = false,
)
