package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.Earthquake
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Provides the locally observed recent-earthquake list and refreshes its source data. */
interface EarthquakeRepository {
    /** Emits the rolling 24-hour list, ordered by occurrence time with newest first. */
    fun observeEarthquakes(): Flow<List<Earthquake>>

    /** Emits the device time of the last successful source refresh, or null before the first one. */
    fun observeLastSuccessfulRefresh(): Flow<Instant?>

    /** Emits the cached event for this identity, or null while it is unavailable locally. */
    fun observeEarthquake(id: String): Flow<Earthquake?>

    /** Refreshes the source and returns a typed outcome instead of leaking infrastructure errors. */
    suspend fun refresh(): RefreshResult

    /** Fetches details by ID only when the event is not already cached. */
    suspend fun fetchEarthquakeById(id: String): EarthquakeLookupResult
}
