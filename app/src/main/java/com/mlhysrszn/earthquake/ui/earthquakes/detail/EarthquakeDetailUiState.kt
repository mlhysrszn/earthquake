package com.mlhysrszn.earthquake.ui.earthquakes.detail

import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.RefreshResult

sealed interface EarthquakeDetailUiState {
    data object Loading : EarthquakeDetailUiState

    data class Content(val earthquake: Earthquake) : EarthquakeDetailUiState

    data object Unavailable : EarthquakeDetailUiState

    data class Failure(val reason: RefreshResult.Reason) : EarthquakeDetailUiState
}
