package com.mlhysrszn.earthquake.ui.earthquakes.list

import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.time.Instant

data class EarthquakesUiState(
    val earthquakes: List<Earthquake> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val refreshError: RefreshResult.Reason? = null,
    val lastUpdatedAt: Instant? = null,
)
