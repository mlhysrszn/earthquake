package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

interface NotificationPreferencesRepository {
    fun observePreferences(): Flow<NotificationPreferences>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setMagnitudeThreshold(threshold: Double)
}
