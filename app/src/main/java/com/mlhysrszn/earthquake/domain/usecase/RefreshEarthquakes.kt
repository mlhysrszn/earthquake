package com.mlhysrszn.earthquake.domain.usecase

import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin

/** Refreshes earthquake data and applies any notification decisions for that snapshot. */
fun interface RefreshEarthquakes {
    suspend fun refresh(origin: RefreshOrigin): RefreshResult
}
