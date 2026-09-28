package com.mlhysrszn.earthquake.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.domain.model.isValidMagnitudeThreshold
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreNotificationPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : NotificationPreferencesRepository {
    override fun observePreferences(): Flow<NotificationPreferences> = dataStore.data.map { values ->
        val storedThresholdTenths = values[MAGNITUDE_THRESHOLD_TENTHS_KEY]
            ?.takeIf(::isValidThresholdTenths)
            ?: DEFAULT_MAGNITUDE_THRESHOLD_TENTHS

        NotificationPreferences(
            notificationsEnabled =
                values[NOTIFICATIONS_ENABLED_KEY]
                    ?: NotificationPreferences.DEFAULT_NOTIFICATIONS_ENABLED,
            magnitudeThreshold = storedThresholdTenths / 10.0,
        )
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { values ->
            values[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    override suspend fun setMagnitudeThreshold(threshold: Double) {
        require(isValidMagnitudeThreshold(threshold)) {
            "Magnitude threshold is outside the supported range or step"
        }
        val thresholdTenths = (threshold * 10).toInt()
        dataStore.edit { values ->
            values[MAGNITUDE_THRESHOLD_TENTHS_KEY] = thresholdTenths
        }
    }

    private fun isValidThresholdTenths(value: Int): Boolean =
        value in MIN_MAGNITUDE_THRESHOLD_TENTHS..MAX_MAGNITUDE_THRESHOLD_TENTHS &&
            value % MAGNITUDE_THRESHOLD_STEP_TENTHS == 0

    private companion object {
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val MAGNITUDE_THRESHOLD_TENTHS_KEY = intPreferencesKey("magnitude_threshold_tenths")

        const val DEFAULT_MAGNITUDE_THRESHOLD_TENTHS = 40
        const val MIN_MAGNITUDE_THRESHOLD_TENTHS = 0
        const val MAX_MAGNITUDE_THRESHOLD_TENTHS = 95
        const val MAGNITUDE_THRESHOLD_STEP_TENTHS = 5
    }
}
