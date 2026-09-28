package com.mlhysrszn.earthquake.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlhysrszn.earthquake.domain.model.isValidMagnitudeThreshold
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationPreferencesRepository,
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
        persist { repository.setNotificationsEnabled(enabled) }
    }

    fun setMagnitudeThreshold(threshold: Double) {
        if (!isValidMagnitudeThreshold(threshold)) {
            mutableSaveFailed.value = true
            return
        }
        persist { repository.setMagnitudeThreshold(threshold) }
    }

    private fun persist(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
                mutableSaveFailed.value = false
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableSaveFailed.value = true
            }
        }
    }
}
