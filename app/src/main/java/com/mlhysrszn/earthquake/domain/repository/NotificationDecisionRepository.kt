package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.NotificationDecision
import kotlinx.coroutines.flow.Flow

/** Read-only view of the notification decisions stored by the processor. */
interface NotificationDecisionRepository {
    /** Newest decisions first. */
    fun observeRecentDecisions(limit: Int): Flow<List<NotificationDecision>>
}
