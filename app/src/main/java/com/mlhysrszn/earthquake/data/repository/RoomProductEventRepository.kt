package com.mlhysrszn.earthquake.data.repository

import com.mlhysrszn.earthquake.data.local.room.ProductEventDao
import com.mlhysrszn.earthquake.data.local.room.ProductEventEntity
import com.mlhysrszn.earthquake.data.local.room.toDomain
import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.repository.ProductEventRepository
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomProductEventRepository @Inject constructor(
    private val productEventDao: ProductEventDao,
) : ProductEventRepository {
    private val json = Json

    override suspend fun record(event: ProductEvent) {
        val stableProperties: Map<String, String> = event.properties.toSortedMap().toMap()
        productEventDao.record(
            event = ProductEventEntity(
                id = event.id,
                name = event.name.name,
                occurredAtEpochMillis = event.occurredAt.toEpochMilli(),
                environment = event.environment.name,
                propertiesJson = json.encodeToString<Map<String, String>>(stableProperties),
            ),
            retainedSinceEpochMillis = event.occurredAt.toEpochMilli() - EVENT_RETENTION.toMillis(),
        )
    }

    override fun observeRecentEvents(limit: Int): Flow<List<ProductEvent>> {
        require(limit > 0)
        return productEventDao.observeRecent(limit).map { entities -> entities.map(::toDomain) }
    }

    override suspend fun getRecentEvents(limit: Int): List<ProductEvent> {
        require(limit > 0)
        return productEventDao.getRecent(limit).map(::toDomain)
    }

    private fun toDomain(entity: ProductEventEntity): ProductEvent = try {
        entity.toDomain(json.decodeFromString<Map<String, String>>(entity.propertiesJson))
    } catch (_: Exception) {
        entity.toDomain(emptyMap())
    }

    private companion object {
        val EVENT_RETENTION: Duration = Duration.ofDays(90)
    }
}
