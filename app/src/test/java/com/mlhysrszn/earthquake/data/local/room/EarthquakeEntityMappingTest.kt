package com.mlhysrszn.earthquake.data.local.room

import com.mlhysrszn.earthquake.domain.model.Earthquake
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EarthquakeEntityMappingTest {
    @Test
    fun `maps domain fields and UTC instants to and from epoch milliseconds`() {
        val earthquake = Earthquake(
            id = "usgs-event-1",
            magnitude = -0.4,
            magnitudeType = "md",
            place = "Near Alaska",
            occurredAt = Instant.parse("2026-09-28T10:30:00.123Z"),
            updatedAt = Instant.parse("2026-09-28T10:31:00.456Z"),
            longitude = -149.0,
            latitude = 61.0,
            depthKm = 20.0,
            sourceUrl = "https://earthquake.usgs.gov/example",
        )

        val entity = earthquake.toEntity()

        assertEquals(earthquake, entity.toDomain())
        assertEquals(earthquake.occurredAt.toEpochMilli(), entity.occurredAtEpochMillis)
        assertEquals(earthquake.updatedAt?.toEpochMilli(), entity.updatedAtEpochMillis)
    }

    @Test
    fun `preserves nullable values`() {
        val earthquake = Earthquake(
            id = "usgs-event-2",
            magnitude = null,
            magnitudeType = null,
            place = null,
            occurredAt = Instant.parse("2026-09-28T10:30:00Z"),
            updatedAt = null,
            longitude = null,
            latitude = null,
            depthKm = null,
            sourceUrl = null,
        )

        val entity = earthquake.toEntity()

        assertEquals(earthquake, entity.toDomain())
        assertNull(entity.updatedAtEpochMillis)
    }
}
