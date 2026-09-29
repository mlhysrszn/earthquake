package com.mlhysrszn.earthquake.data.analytics

import android.util.Log
import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import com.mlhysrszn.earthquake.domain.repository.ProductEventRepository
import com.mlhysrszn.earthquake.di.AppVariantConfiguration
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Stores anonymous product events locally; recorder failures never block app behavior. */
@Singleton
class LocalProductEventRecorder @Inject constructor(
    private val repository: ProductEventRepository,
    private val clock: Clock,
    private val variantConfiguration: AppVariantConfiguration,
) : ProductEventRecorder {
    override suspend fun record(
        name: ProductEventName,
        properties: Map<String, String>,
    ) {
        try {
            repository.record(
                ProductEvent(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    occurredAt = clock.instant(),
                    environment = variantConfiguration.environment,
                    properties = properties.toMap(),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Log.w(TAG, "Could not persist local product event: ${name.name}", exception)
        }
    }

    private companion object {
        const val TAG = "LocalProductEventRecorder"
    }
}
