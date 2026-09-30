package com.mlhysrszn.earthquake.ui.earthquakes.list

import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.time.Instant

data class EarthquakesUiState(
    /** Events that pass [minimumMagnitude]; events without a magnitude are hidden by any filter. */
    val earthquakes: List<Earthquake> = emptyList(),
    /** Number of events in the rolling window before the magnitude filter is applied. */
    val totalEarthquakeCount: Int = 0,
    val minimumMagnitude: Double? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val refreshError: RefreshResult.Reason? = null,
    /** Device time of the last successful refresh, persisted across process restarts. */
    val lastUpdatedAt: Instant? = null,
    /** Time used to render relative ages such as "5 dk önce"; taken when the state is built. */
    val referenceTime: Instant? = null,
)
