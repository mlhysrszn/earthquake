package com.mlhysrszn.earthquake.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EarthquakeTest {
    @Test
    fun `accepts nullable fields and a negative finite magnitude`() {
        val earthquake = earthquake(
            magnitude = -0.5,
            magnitudeType = null,
            place = null,
            updatedAt = null,
            longitude = null,
            latitude = null,
            depthKm = null,
            sourceUrl = null,
        )

        assertEquals(-0.5, earthquake.magnitude!!, 0.0)
        assertNull(earthquake.place)
        assertNull(earthquake.longitude)
    }

    @Test
    fun `rejects a blank identity`() {
        assertThrows(IllegalArgumentException::class.java) {
            earthquake(id = "  ")
        }
    }

    @Test
    fun `rejects a non-finite magnitude`() {
        assertThrows(IllegalArgumentException::class.java) {
            earthquake(magnitude = Double.NaN)
        }
    }

    @Test
    fun `requires coordinates as a valid pair`() {
        assertThrows(IllegalArgumentException::class.java) {
            earthquake(longitude = 12.0, latitude = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            earthquake(longitude = 181.0, latitude = 12.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            earthquake(longitude = 12.0, latitude = -91.0)
        }
    }

    @Test
    fun `rejects non-finite depth`() {
        assertThrows(IllegalArgumentException::class.java) {
            earthquake(depthKm = Double.POSITIVE_INFINITY)
        }
    }

    private fun earthquake(
        id: String = "usgs-event-1",
        magnitude: Double? = 4.2,
        magnitudeType: String? = "ml",
        place: String? = "Near the coast",
        occurredAt: Instant = Instant.parse("2026-09-28T10:00:00Z"),
        updatedAt: Instant? = Instant.parse("2026-09-28T10:01:00Z"),
        longitude: Double? = -122.0,
        latitude: Double? = 37.0,
        depthKm: Double? = 8.5,
        sourceUrl: String? = "https://example.test/event",
    ) = Earthquake(
        id = id,
        magnitude = magnitude,
        magnitudeType = magnitudeType,
        place = place,
        occurredAt = occurredAt,
        updatedAt = updatedAt,
        longitude = longitude,
        latitude = latitude,
        depthKm = depthKm,
        sourceUrl = sourceUrl,
    )
}
