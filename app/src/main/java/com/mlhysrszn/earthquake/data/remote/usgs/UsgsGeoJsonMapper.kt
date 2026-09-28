package com.mlhysrszn.earthquake.data.remote.usgs

import com.mlhysrszn.earthquake.domain.model.Earthquake
import java.time.DateTimeException
import java.time.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject

data class MappedUsgsFeed(
    val earthquakes: List<Earthquake>,
    val sourceGeneratedAt: Instant?,
)

class MalformedUsgsFeedException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

class UsgsGeoJsonMapper @Inject constructor(
    private val json: Json,
) {
    fun decodeSingleEvent(payload: String): Earthquake? {
        val root = try {
            json.parseToJsonElement(payload) as? JsonObject
                ?: throw MalformedUsgsFeedException("USGS detail response must be a JSON object")
        } catch (exception: SerializationException) {
            throw MalformedUsgsFeedException("USGS detail response is not valid GeoJSON", exception)
        }
        val rootType = (root["type"] as? JsonPrimitive)?.contentOrNull

        return when (rootType) {
            FEATURE_TYPE -> decodeFeature(root)
            FEATURE_COLLECTION_TYPE -> {
                val earthquakes = decode(payload).earthquakes
                when (earthquakes.size) {
                    0 -> null
                    1 -> earthquakes.single()
                    else -> throw MalformedUsgsFeedException(
                        "USGS event query returned more than one earthquake",
                    )
                }
            }

            else -> throw MalformedUsgsFeedException(
                "USGS detail response is neither a Feature nor a FeatureCollection",
            )
        }
    }

    fun decode(payload: String): MappedUsgsFeed {
        val feed = try {
            json.decodeFromString<UsgsFeatureCollectionDto>(payload)
        } catch (exception: SerializationException) {
            throw MalformedUsgsFeedException("USGS response is not valid GeoJSON", exception)
        }

        if (feed.type != FEATURE_COLLECTION_TYPE) {
            throw MalformedUsgsFeedException("USGS response is not a FeatureCollection")
        }
        val features = feed.features
            ?: throw MalformedUsgsFeedException("USGS FeatureCollection has no features array")

        return MappedUsgsFeed(
            earthquakes = features.mapNotNull(::mapFeature)
                .sortedByDescending(Earthquake::occurredAt),
            sourceGeneratedAt = feed.metadata?.generated?.let(::instantOrNull),
        )
    }

    private fun mapFeature(feature: UsgsFeatureDto): Earthquake? {
        if (feature.type != FEATURE_TYPE) return null

        val id = feature.id?.takeIf(String::isNotBlank) ?: return null
        val properties = feature.properties ?: return null
        if (properties.type != EARTHQUAKE_TYPE) return null

        val occurredAt = properties.time?.let(::instantOrNull) ?: return null
        val magnitude = properties.mag
        if (magnitude != null && !magnitude.isFinite()) return null

        val coordinates = mapCoordinates(feature.geometry?.coordinates)
        return Earthquake(
            id = id,
            magnitude = magnitude,
            magnitudeType = properties.magnitudeType,
            place = properties.place,
            occurredAt = occurredAt,
            updatedAt = properties.updated?.let(::instantOrNull),
            longitude = coordinates.longitude,
            latitude = coordinates.latitude,
            depthKm = coordinates.depthKm,
            sourceUrl = properties.url,
        )
    }

    private fun decodeFeature(feature: JsonObject): Earthquake? {
        val dto = try {
            json.decodeFromJsonElement<UsgsFeatureDto>(feature)
        } catch (exception: SerializationException) {
            throw MalformedUsgsFeedException("USGS detail Feature is malformed", exception)
        }
        return mapFeature(dto)
    }

    private fun mapCoordinates(values: List<Double>?): MappedCoordinates {
        val longitude = values?.getOrNull(LONGITUDE_INDEX)
        val latitude = values?.getOrNull(LATITUDE_INDEX)
        val validPair = longitude != null && latitude != null &&
            longitude.isFinite() && longitude in MIN_LONGITUDE..MAX_LONGITUDE &&
            latitude.isFinite() && latitude in MIN_LATITUDE..MAX_LATITUDE

        return MappedCoordinates(
            longitude = longitude.takeIf { validPair },
            latitude = latitude.takeIf { validPair },
            depthKm = values?.getOrNull(DEPTH_INDEX)?.takeIf(Double::isFinite),
        )
    }

    private fun instantOrNull(epochMillis: Long): Instant? = try {
        Instant.ofEpochMilli(epochMillis)
    } catch (_: DateTimeException) {
        null
    }

    private data class MappedCoordinates(
        val longitude: Double?,
        val latitude: Double?,
        val depthKm: Double?,
    )

    private companion object {
        const val FEATURE_COLLECTION_TYPE = "FeatureCollection"
        const val FEATURE_TYPE = "Feature"
        const val EARTHQUAKE_TYPE = "earthquake"
        const val LONGITUDE_INDEX = 0
        const val LATITUDE_INDEX = 1
        const val DEPTH_INDEX = 2
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
    }
}
