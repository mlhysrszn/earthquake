package com.mlhysrszn.earthquake.ui.earthquakes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
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
) : ViewModel() {
    private val mutableRefreshState = MutableStateFlow(EarthquakesUiState())
    private var refreshJob: Job? = null
    private var initialLoadCompleted = false

    val uiState: StateFlow<EarthquakesUiState> = combine(
        repository.observeEarthquakes(),
        mutableRefreshState,
    ) { earthquakes, refreshState ->
        refreshState.copy(earthquakes = earthquakes)
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
                when (result) {
                    is RefreshResult.Success -> currentState.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        refreshError = null,
                        lastUpdatedAt = clock.instant(),
                    )

                    is RefreshResult.Failure -> currentState.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        refreshError = result.reason,
                    )
                }
            }
        }
    }
}
