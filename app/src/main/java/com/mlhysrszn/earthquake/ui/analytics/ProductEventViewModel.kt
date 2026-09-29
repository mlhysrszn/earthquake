package com.mlhysrszn.earthquake.ui.analytics

import androidx.lifecycle.ViewModel
import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProductEventViewModel @Inject constructor(
    private val recorder: ProductEventRecorder,
) : ViewModel() {
    suspend fun recordScreenView(
        screen: String,
        properties: Map<String, String> = emptyMap(),
        environment: ProductEventEnvironment = ProductEventEnvironment.LIVE,
    ) = recorder.record(
        name = ProductEventName.SCREEN_VIEW,
        properties = properties + ("screen" to screen),
        environment = environment,
    )

    suspend fun recordNotificationOpened(
        eventId: String,
        environment: ProductEventEnvironment = ProductEventEnvironment.LIVE,
    ) = recorder.record(
        name = ProductEventName.NOTIFICATION_OPENED,
        properties = mapOf("event_id" to eventId),
        environment = environment,
    )
}
