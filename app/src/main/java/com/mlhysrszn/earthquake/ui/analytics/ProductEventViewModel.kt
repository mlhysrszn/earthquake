package com.mlhysrszn.earthquake.ui.analytics

import androidx.lifecycle.ViewModel
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
    ) = recorder.record(
        name = ProductEventName.SCREEN_VIEW,
        properties = properties + ("screen" to screen),
    )

    suspend fun recordNotificationOpened(eventId: String) = recorder.record(
        name = ProductEventName.NOTIFICATION_OPENED,
        properties = mapOf("event_id" to eventId),
    )
}
