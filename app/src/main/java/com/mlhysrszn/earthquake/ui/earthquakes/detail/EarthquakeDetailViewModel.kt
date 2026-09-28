package com.mlhysrszn.earthquake.ui.earthquakes.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EarthquakeDetailViewModel.Factory::class)
class EarthquakeDetailViewModel @AssistedInject constructor(
    @Assisted val earthquakeId: String,
    private val repository: EarthquakeRepository,
) : ViewModel() {
    private val mutableLookupState = MutableStateFlow<EarthquakeDetailUiState>(
        EarthquakeDetailUiState.Loading,
    )
    private var lookupJob: Job? = null

    val uiState: StateFlow<EarthquakeDetailUiState> = combine(
        repository.observeEarthquake(earthquakeId),
        mutableLookupState,
    ) { cachedEarthquake, lookupState ->
        cachedEarthquake?.let(EarthquakeDetailUiState::Content) ?: lookupState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = EarthquakeDetailUiState.Loading,
    )

    init {
        retry()
    }

    fun retry() {
        if (lookupJob?.isActive == true) return
        lookupJob = viewModelScope.launch {
            mutableLookupState.value = EarthquakeDetailUiState.Loading
            mutableLookupState.value = try {
                when (val result = repository.fetchEarthquakeById(earthquakeId)) {
                    is EarthquakeLookupResult.Found ->
                        EarthquakeDetailUiState.Content(result.earthquake)

                    EarthquakeLookupResult.Unavailable -> EarthquakeDetailUiState.Unavailable
                    is EarthquakeLookupResult.Failure ->
                        EarthquakeDetailUiState.Failure(result.reason)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                EarthquakeDetailUiState.Failure(RefreshResult.Reason.UNKNOWN)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(earthquakeId: String): EarthquakeDetailViewModel
    }
}
