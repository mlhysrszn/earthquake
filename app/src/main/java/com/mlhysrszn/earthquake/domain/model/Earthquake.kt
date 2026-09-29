package com.mlhysrszn.earthquake.domain.model

import java.time.Instant

/** A validated earthquake record independent of API and database representations. */
data class Earthquake(
    val id: String,
    val magnitude: Double?,
    val magnitudeType: String?,
    val place: String?,
    val occurredAt: Instant,
    val updatedAt: Instant?,
    val longitude: Double?,
    val latitude: Double?,
    val depthKm: Double?,
    val sourceUrl: String?,
    val aliases: Set<String> = emptySet(),
) {
    init {
        require(id.isNotBlank()) { "Earthquake ID must not be blank" }
        require(magnitude == null || magnitude.isFinite()) {
            "Magnitude must be finite when present"
        }
        require((longitude == null) == (latitude == null)) {
            "Longitude and latitude must either both be present or both be absent"
        }
        if (longitude != null && latitude != null) {
            require(longitude.isFinite() && longitude in -180.0..180.0) {
                "Longitude must be finite and between -180 and 180 degrees"
            }
            require(latitude.isFinite() && latitude in -90.0..90.0) {
                "Latitude must be finite and between -90 and 90 degrees"
            }
        }
        require(depthKm == null || depthKm.isFinite()) {
            "Depth must be finite when present"
        }
    }
}
