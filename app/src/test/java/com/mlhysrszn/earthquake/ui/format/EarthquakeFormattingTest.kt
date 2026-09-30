package com.mlhysrszn.earthquake.ui.format

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class EarthquakeFormattingTest {
    private val reference = Instant.parse("2026-09-30T12:00:00Z")

    @Test
    fun `magnitude and coordinates use a Turkish decimal comma`() {
        assertEquals("4,0", formatMagnitude(4.0))
        assertEquals("38,123", formatCoordinate(38.1234))
    }

    @Test
    fun `occurrence time uses Turkish month names in the given zone`() {
        assertEquals("30 Eyl, 15:00", formatOccurrenceTime(reference, ZoneOffset.ofHours(3)))
    }

    @Test
    fun `relative age rounds down to minutes then hours`() {
        assertEquals(RelativeAge.JustNow, relativeAge(reference.minusSeconds(59), reference))
        assertEquals(RelativeAge.JustNow, relativeAge(reference.plusSeconds(30), reference))
        assertEquals(RelativeAge.Minutes(1), relativeAge(reference.minusSeconds(60), reference))
        assertEquals(RelativeAge.Minutes(59), relativeAge(reference.minusSeconds(3_599), reference))
        assertEquals(RelativeAge.Hours(1), relativeAge(reference.minusSeconds(3_600), reference))
        assertEquals(RelativeAge.Hours(23), relativeAge(reference.minusSeconds(86_000), reference))
    }

    @Test
    fun `severity bands have inclusive lower bounds`() {
        assertEquals(MagnitudeSeverity.UNKNOWN, magnitudeSeverity(null))
        assertEquals(MagnitudeSeverity.MINOR, magnitudeSeverity(2.9))
        assertEquals(MagnitudeSeverity.LIGHT, magnitudeSeverity(3.0))
        assertEquals(MagnitudeSeverity.MODERATE, magnitudeSeverity(4.0))
        assertEquals(MagnitudeSeverity.STRONG, magnitudeSeverity(5.0))
    }
}
