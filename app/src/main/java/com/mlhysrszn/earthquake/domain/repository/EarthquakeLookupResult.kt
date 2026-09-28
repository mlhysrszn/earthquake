package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.Earthquake

sealed interface EarthquakeLookupResult {
    data class Found(val earthquake: Earthquake) : EarthquakeLookupResult

    data object Unavailable : EarthquakeLookupResult

    data class Failure(val reason: RefreshResult.Reason) : EarthquakeLookupResult
}
