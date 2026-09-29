package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import java.time.Instant

@Entity(
    tableName = "product_events",
    indices = [
        Index(value = ["occurredAtEpochMillis"]),
        Index(value = ["name", "occurredAtEpochMillis"]),
    ],
)
data class ProductEventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val occurredAtEpochMillis: Long,
    val environment: String,
    val propertiesJson: String,
)

fun ProductEventEntity.toDomain(properties: Map<String, String>) = ProductEvent(
    id = id,
    name = ProductEventName.valueOf(name),
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
    environment = ProductEventEnvironment.valueOf(environment),
    properties = properties,
)
