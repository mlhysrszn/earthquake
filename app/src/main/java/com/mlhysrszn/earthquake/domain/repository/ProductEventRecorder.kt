package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import com.mlhysrszn.earthquake.domain.model.ProductEventName

/** Best-effort recording boundary so local telemetry failures never change product behavior. */
fun interface ProductEventRecorder {
    suspend fun record(
        name: ProductEventName,
        properties: Map<String, String>,
        environment: ProductEventEnvironment,
    )

    suspend fun record(
        name: ProductEventName,
        properties: Map<String, String> = emptyMap(),
    ) = record(name, properties, ProductEventEnvironment.LIVE)
}
