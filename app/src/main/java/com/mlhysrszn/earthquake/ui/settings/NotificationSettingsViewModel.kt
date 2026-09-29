package com.mlhysrszn.earthquake.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlhysrszn.earthquake.domain.model.isValidMagnitudeThreshold
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationPreferencesRepository,
    private val productEventRecorder: ProductEventRecorder,
) : ViewModel() {
    private val mutableSaveFailed = MutableStateFlow(false)

    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        repository.observePreferences(),
        mutableSaveFailed,
    ) { preferences, saveFailed ->
        NotificationSettingsUiState(
            preferences = preferences,
            saveFailed = saveFailed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = NotificationSettingsUiState(),
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val attemptId = if (enabled) UUID.randomUUID().toString() else null
            if (attemptId != null) {
                productEventRecorder.record(
                    name = ProductEventName.NOTIFICATION_SETUP_STARTED,
                    properties = mapOf("attempt_id" to attemptId),
                )
            }
            try {
                repository.setNotificationsEnabled(enabled)
                productEventRecorder.record(
                    name = ProductEventName.NOTIFICATION_PREFERENCE_SAVED,
                    properties = mapOf(
                        "preference" to "notifications_enabled",
                        "value" to enabled.toString(),
                    ),
                )
                if (attemptId != null) {
                    productEventRecorder.record(
                        name = ProductEventName.NOTIFICATION_SETUP_COMPLETED,
                        properties = mapOf("attempt_id" to attemptId),
                    )
                }
                mutableSaveFailed.value = false
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableSaveFailed.value = true
            }
        }
    }

    fun setMagnitudeThreshold(threshold: Double) {
        if (!isValidMagnitudeThreshold(threshold)) {
            mutableSaveFailed.value = true
            return
        }
        persist(
            write = { repository.setMagnitudeThreshold(threshold) },
            eventProperties = mapOf(
                "preference" to "magnitude_threshold",
                "value" to threshold.toString(),
            ),
        )
    }

    fun recordPermissionOutcome(status: String, trigger: String) {
        viewModelScope.launch {
            productEventRecorder.record(
                name = ProductEventName.PERMISSION_OUTCOME,
                properties = mapOf("status" to status, "trigger" to trigger),
            )
        }
    }

    private fun persist(
        write: suspend () -> Unit,
        eventProperties: Map<String, String>,
    ) {
        viewModelScope.launch {
            try {
                write()
                productEventRecorder.record(
                    name = ProductEventName.NOTIFICATION_PREFERENCE_SAVED,
                    properties = eventProperties,
                )
                mutableSaveFailed.value = false
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableSaveFailed.value = true
            }
        }
    }
}
