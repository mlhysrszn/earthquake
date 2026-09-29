package com.mlhysrszn.earthquake.data.remote.usgs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UsgsFeatureCollectionDto(
    val type: String? = null,
    val metadata: UsgsMetadataDto? = null,
    val features: List<UsgsFeatureDto>? = null,
)

@Serializable
internal data class UsgsMetadataDto(
    val generated: Long? = null,
)

@Serializable
internal data class UsgsFeatureDto(
    val type: String? = null,
    val id: String? = null,
    val properties: UsgsPropertiesDto? = null,
    val geometry: UsgsGeometryDto? = null,
)

@Serializable
internal data class UsgsPropertiesDto(
    val mag: Double? = null,
    val place: String? = null,
    val time: Long? = null,
    val updated: Long? = null,
    val url: String? = null,
    @SerialName("magType") val magnitudeType: String? = null,
    val type: String? = null,
    val ids: String? = null,
)

@Serializable
internal data class UsgsGeometryDto(
    val coordinates: List<Double>? = null,
)
