package com.mlhysrszn.earthquake.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mlhysrszn.earthquake.domain.model.Earthquake
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "earthquakes",
    indices = [Index(value = ["occurredAtEpochMillis"])],
)
data class EarthquakeEntity(
    @PrimaryKey val id: String,
    val magnitude: Double?,
    val magnitudeType: String?,
    val place: String?,
    val occurredAtEpochMillis: Long,
    val updatedAtEpochMillis: Long?,
    val longitude: Double?,
    val latitude: Double?,
    val depthKm: Double?,
    val sourceUrl: String?,
    @ColumnInfo(defaultValue = "'[]'") val aliasIdsJson: String = "[]",
)

fun Earthquake.toEntity(): EarthquakeEntity = EarthquakeEntity(
    id = id,
    magnitude = magnitude,
    magnitudeType = magnitudeType,
    place = place,
    occurredAtEpochMillis = occurredAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt?.toEpochMilli(),
    longitude = longitude,
    latitude = latitude,
    depthKm = depthKm,
    sourceUrl = sourceUrl,
    aliasIdsJson = aliases.toJsonString(),
)

fun EarthquakeEntity.toDomain(): Earthquake = Earthquake(
    id = id,
    magnitude = magnitude,
    magnitudeType = magnitudeType,
    place = place,
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
    updatedAt = updatedAtEpochMillis?.let(Instant::ofEpochMilli),
    longitude = longitude,
    latitude = latitude,
    depthKm = depthKm,
    sourceUrl = sourceUrl,
    aliases = aliasIdsJson.toAliasSet(),
)

private fun Set<String>.toJsonString(): String =
    Json.encodeToString(sorted())

private fun String.toAliasSet(): Set<String> =
    Json.decodeFromString<List<String>>(this).toSet()
