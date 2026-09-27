package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.Earthquake
import kotlinx.coroutines.flow.Flow

/** Provides the locally observed recent-earthquake list and refreshes its source data. */
interface EarthquakeRepository {
    /** Emits the rolling 24-hour list, ordered by occurrence time with newest first. */
    fun observeEarthquakes(): Flow<List<Earthquake>>

    /** Refreshes the source and returns a typed outcome instead of leaking infrastructure errors. */
    suspend fun refresh(): RefreshResult
}
