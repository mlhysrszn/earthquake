package com.mlhysrszn.earthquake.ui.earthquakes.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EarthquakesViewModel @Inject constructor(
    private val repository: EarthquakeRepository,
    private val refreshEarthquakes: RefreshEarthquakes,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableRefreshState = MutableStateFlow(EarthquakesUiState())
    private val minimumMagnitude = savedStateHandle.getStateFlow<Double?>(MINIMUM_MAGNITUDE_KEY, null)
    private var refreshJob: Job? = null
    private var initialLoadCompleted = false

    val uiState: StateFlow<EarthquakesUiState> = combine(
        repository.observeEarthquakes(),
        repository.observeLastSuccessfulRefresh(),
        mutableRefreshState,
        minimumMagnitude,
    ) { earthquakes, lastSuccessfulRefresh, refreshState, minimum ->
        refreshState.copy(
            earthquakes = earthquakes.filter { earthquake ->
                minimum == null || (earthquake.magnitude ?: return@filter false) >= minimum
            },
            totalEarthquakeCount = earthquakes.size,
            minimumMagnitude = minimum,
            lastUpdatedAt = lastSuccessfulRefresh,
            referenceTime = clock.instant(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = mutableRefreshState.value,
    )

    init {
        requestRefresh()
    }

    fun refresh() {
        requestRefresh()
    }

    fun retry() {
        requestRefresh()
    }

    /** Called when the screen resumes; refreshes only when the data is older than [STALE_AFTER]. */
    fun refreshIfStale() {
        val lastUpdatedAt = uiState.value.lastUpdatedAt
        if (lastUpdatedAt != null && Duration.between(lastUpdatedAt, clock.instant()) < STALE_AFTER) {
            return
        }
        requestRefresh()
    }

    fun setMinimumMagnitude(minimum: Double?) {
        savedStateHandle[MINIMUM_MAGNITUDE_KEY] = minimum
    }

    private fun requestRefresh() {
        if (refreshJob?.isActive == true) return
        val isInitialLoad = !initialLoadCompleted
        refreshJob = viewModelScope.launch {
            mutableRefreshState.update {
                it.copy(
                    isInitialLoading = isInitialLoad,
                    isRefreshing = !isInitialLoad,
                    refreshError = null,
                )
            }
            val result = try {
                refreshEarthquakes.refresh(RefreshOrigin.FOREGROUND)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
            }
            initialLoadCompleted = true
            mutableRefreshState.update { currentState ->
                currentState.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    refreshError = (result as? RefreshResult.Failure)?.reason,
                )
            }
        }
    }

    companion object {
        /** Filter choices shown on the list; null shows every event. */
        val MAGNITUDE_FILTERS: List<Double?> = listOf(null, 3.0, 4.0, 5.0)
        val STALE_AFTER: Duration = Duration.ofMinutes(5)
        private const val MINIMUM_MAGNITUDE_KEY = "minimum_magnitude"
    }
}
