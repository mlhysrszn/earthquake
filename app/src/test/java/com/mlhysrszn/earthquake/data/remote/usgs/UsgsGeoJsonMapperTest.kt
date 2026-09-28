package com.mlhysrszn.earthquake.data.remote.usgs

import com.mlhysrszn.earthquake.domain.model.Earthquake
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class UsgsGeoJsonMapperTest {
    private val mapper = UsgsGeoJsonMapper(Json { ignoreUnknownKeys = true })

    @Test
    fun `maps fields sorts newest first and preserves coordinate order and negative magnitude`() {
        val feed = mapper.decode(fixture("summary.json"))

        assertEquals(2, feed.earthquakes.size)
        assertEquals("usgs-missing-optional-fields", feed.earthquakes[0].id)
        assertEquals("usgs-negative-magnitude", feed.earthquakes[1].id)
        assertEquals(-0.3, feed.earthquakes[1].magnitude!!, 0.0)
        assertEquals(-149.0, feed.earthquakes[1].longitude!!, 0.0)
        assertEquals(61.0, feed.earthquakes[1].latitude!!, 0.0)
        assertEquals(20.0, feed.earthquakes[1].depthKm!!, 0.0)
        assertEquals("md", feed.earthquakes[1].magnitudeType)
        assertEquals(Instant.ofEpochMilli(1790596801000), feed.earthquakes[1].updatedAt)
        assertEquals(Instant.ofEpochMilli(1790596800000), feed.sourceGeneratedAt)
    }

    @Test
    fun `missing magnitude and place remain nullable`() {
        val earthquake = mapper.decode(fixture("summary.json")).earthquakes
            .first { it.id == "usgs-missing-optional-fields" }

        assertNull(earthquake.magnitude)
        assertNull(earthquake.place)
        assertNull(earthquake.magnitudeType)
        assertNull(earthquake.depthKm)
    }

    @Test
    fun `invalid identities and occurrence times skip only those records`() {
        val payload = """
            {
              "type": "FeatureCollection",
              "features": [
                {"type":"Feature","id":"missing-time","properties":{"type":"earthquake"}},
                {"type":"Feature","id":"  ","properties":{"time":1790596800000,"type":"earthquake"}},
                {"type":"Feature","properties":{"time":1790596800000,"type":"earthquake"}},
                {"type":"Feature","id":"valid","properties":{"time":1790596800000,"type":"earthquake"}}
              ]
            }
        """.trimIndent()

        val events = mapper.decode(payload).earthquakes

        assertEquals(listOf("valid"), events.map(Earthquake::id))
    }

    @Test
    fun `invalid coordinate pairs are omitted without rejecting the event`() {
        val payload = """
            {
              "type": "FeatureCollection",
              "features": [
                {"type":"Feature","id":"outside-range","properties":{"time":1790596800000,"type":"earthquake"},"geometry":{"coordinates":[181.0,91.0,4.0]}},
                {"type":"Feature","id":"partial-pair","properties":{"time":1790596800000,"type":"earthquake"},"geometry":{"coordinates":[12.0]}},
                {"type":"Feature","id":"valid-pair","properties":{"time":1790596800000,"type":"earthquake"},"geometry":{"coordinates":[-122.0,37.0,6.0]}}
              ]
            }
        """.trimIndent()

        val events = mapper.decode(payload).earthquakes.associateBy(Earthquake::id)

        assertNull(events.getValue("outside-range").longitude)
        assertNull(events.getValue("outside-range").latitude)
        assertEquals(4.0, events.getValue("outside-range").depthKm!!, 0.0)
        assertNull(events.getValue("partial-pair").longitude)
        assertNull(events.getValue("partial-pair").latitude)
        assertEquals(-122.0, events.getValue("valid-pair").longitude!!, 0.0)
        assertEquals(37.0, events.getValue("valid-pair").latitude!!, 0.0)
    }

    @Test
    fun `malformed json and invalid feed envelopes fail as malformed payloads`() {
        assertThrows(MalformedUsgsFeedException::class.java) {
            mapper.decode("{")
        }
        assertThrows(MalformedUsgsFeedException::class.java) {
            mapper.decode("""{"type":"Feature","features":[]}""")
        }
        assertThrows(MalformedUsgsFeedException::class.java) {
            mapper.decode("""{"type":"FeatureCollection"}""")
        }
    }

    @Test
    fun `single event endpoint accepts a Feature or a one-record FeatureCollection`() {
        val feature = """
            {
              "type":"Feature",
              "id":"detail-event",
              "properties":{"mag":3.7,"time":1790596800000,"type":"earthquake"},
              "geometry":{"coordinates":[-120.0,35.0,7.0]}
            }
        """.trimIndent()
        val oneRecordCollection = """
            {"type":"FeatureCollection","features":[$feature]}
        """.trimIndent()

        assertEquals("detail-event", mapper.decodeSingleEvent(feature)?.id)
        assertEquals("detail-event", mapper.decodeSingleEvent(oneRecordCollection)?.id)
    }

    @Test
    fun `single event endpoint maps empty results to unavailable and rejects ambiguity`() {
        assertNull(mapper.decodeSingleEvent("""{"type":"FeatureCollection","features":[]}"""))

        val duplicateFeature = """
            {"type":"Feature","id":"event","properties":{"time":1790596800000,"type":"earthquake"}}
        """.trimIndent()
        val ambiguousCollection = """
            {"type":"FeatureCollection","features":[$duplicateFeature,$duplicateFeature]}
        """.trimIndent()

        assertThrows(MalformedUsgsFeedException::class.java) {
            mapper.decodeSingleEvent(ambiguousCollection)
        }
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("usgs/$name")) {
            "Missing USGS fixture: $name"
        }.readText()
}
