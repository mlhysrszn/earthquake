package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.ProductEvent
import kotlinx.coroutines.flow.Flow

/** On-device product-event history; implementations must not upload records. */
interface ProductEventRepository {
    suspend fun record(event: ProductEvent)

    fun observeRecentEvents(limit: Int = DEFAULT_EVENT_QUERY_LIMIT): Flow<List<ProductEvent>>

    suspend fun getRecentEvents(limit: Int = DEFAULT_EVENT_QUERY_LIMIT): List<ProductEvent>

    companion object {
        const val DEFAULT_EVENT_QUERY_LIMIT = 200
    }
}
