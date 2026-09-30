package com.mlhysrszn.earthquake.ui.activitylog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlhysrszn.earthquake.domain.model.NotificationDecision
import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.repository.NotificationDecisionRepository
import com.mlhysrszn.earthquake.domain.repository.ProductEventRepository
import com.mlhysrszn.earthquake.domain.usecase.ProductMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActivityLogUiState(
    val isLoading: Boolean = true,
    val decisions: List<NotificationDecision> = emptyList(),
    val events: List<ProductEvent> = emptyList(),
    /** Computed from [events], so it covers the same newest [ActivityLogViewModel.EVENT_LIMIT] rows. */
    val metrics: ProductMetrics? = null,
)

/** Shows what the app decided and recorded, straight from the local Room tables. */
@HiltViewModel
class ActivityLogViewModel @Inject constructor(
    decisionRepository: NotificationDecisionRepository,
    productEventRepository: ProductEventRepository,
) : ViewModel() {
    val uiState: StateFlow<ActivityLogUiState> = combine(
        decisionRepository.observeRecentDecisions(DECISION_LIMIT),
        productEventRepository.observeRecentEvents(EVENT_LIMIT),
    ) { decisions, events ->
        ActivityLogUiState(
            isLoading = false,
            decisions = decisions,
            events = events,
            metrics = ProductMetrics.from(events),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ActivityLogUiState(),
    )

    companion object {
        const val DECISION_LIMIT = 200
        const val EVENT_LIMIT = 500
    }
}
