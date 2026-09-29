package com.mlhysrszn.earthquake.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlhysrszn.earthquake.domain.model.DemoScenarioStatus
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin
import com.mlhysrszn.earthquake.domain.repository.DemoScenarioController
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Every action refreshes through the real pipeline so eligibility and delivery rules apply. */
@HiltViewModel
class DemoScenarioViewModel @Inject constructor(
    private val controller: DemoScenarioController,
    private val refreshEarthquakes: RefreshEarthquakes,
) : ViewModel() {
    val status: StateFlow<DemoScenarioStatus> = controller.observeStatus().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = DemoScenarioStatus(isAvailable = false),
    )

    fun addBelowThreshold() = addAndRefresh { controller.addBelowThreshold() }

    fun addAboveThreshold() = addAndRefresh { controller.addAboveThreshold() }

    fun replayDuplicate() {
        viewModelScope.launch {
            if (controller.replayLatestAsDuplicate()) refresh()
        }
    }

    fun reset() {
        viewModelScope.launch {
            controller.reset()
            refresh() // Re-establishes an empty baseline so later events are notifiable.
        }
    }

    private fun addAndRefresh(add: suspend () -> Boolean) {
        viewModelScope.launch {
            // With no stored events the first snapshot becomes the baseline and is never notified.
            if (status.value.eventCount == 0) refresh()
            if (add()) refresh()
        }
    }

    private suspend fun refresh() {
        refreshEarthquakes.refresh(RefreshOrigin.FOREGROUND)
    }
}
