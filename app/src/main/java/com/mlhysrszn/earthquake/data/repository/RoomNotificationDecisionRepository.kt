package com.mlhysrszn.earthquake.data.repository

import com.mlhysrszn.earthquake.data.local.room.NotificationDecisionRow
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingDao
import com.mlhysrszn.earthquake.domain.model.NotificationDecision
import com.mlhysrszn.earthquake.domain.model.NotificationDecisionOutcome
import com.mlhysrszn.earthquake.domain.repository.NotificationDecisionRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomNotificationDecisionRepository @Inject constructor(
    private val processingDao: NotificationProcessingDao,
) : NotificationDecisionRepository {
    override fun observeRecentDecisions(limit: Int): Flow<List<NotificationDecision>> {
        require(limit > 0)
        return processingDao.observeRecentDecisions(limit).map { rows -> rows.map(::toDomain) }
    }

    private fun toDomain(row: NotificationDecisionRow) = NotificationDecision(
        canonicalEventId = row.canonicalEventId,
        eventId = row.eventId,
        occurredAt = Instant.ofEpochMilli(row.eventOccurredAtEpochMillis),
        magnitude = row.magnitude,
        place = row.place,
        outcome = NotificationDecisionOutcome.entries.firstOrNull { it.name == row.outcome }
            ?: NotificationDecisionOutcome.UNKNOWN,
        decidedAt = Instant.ofEpochMilli(row.lastDecisionAtEpochMillis),
    )
}
