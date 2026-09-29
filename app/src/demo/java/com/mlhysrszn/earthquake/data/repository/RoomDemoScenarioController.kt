package com.mlhysrszn.earthquake.data.repository

import com.mlhysrszn.earthquake.data.local.room.DemoScenarioDao
import com.mlhysrszn.earthquake.data.local.room.DemoScenarioEntity
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase
import com.mlhysrszn.earthquake.domain.model.DemoScenarioKind
import com.mlhysrszn.earthquake.domain.model.DemoScenarioStatus
import com.mlhysrszn.earthquake.domain.repository.DemoScenarioController
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Stores scenario events only in the demo database; the repository turns them into a snapshot. */
@Singleton
class RoomDemoScenarioController @Inject constructor(
    private val dao: DemoScenarioDao,
    private val database: EarthquakeDatabase,
    private val clock: Clock,
) : DemoScenarioController {
    override fun observeStatus(): Flow<DemoScenarioStatus> = dao.observeAll().map { entries ->
        val kinds = entries.map { it.kind }.toSet()
        DemoScenarioStatus(
            isAvailable = true,
            eventCount = entries.size,
            hasBelowThreshold = DemoScenarioKind.BELOW_THRESHOLD.name in kinds,
            hasAboveThreshold = DemoScenarioKind.ABOVE_THRESHOLD.name in kinds,
            canReplayDuplicate = entries.isNotEmpty(),
        )
    }

    override suspend fun addBelowThreshold(): Boolean = add(DemoScenarioKind.BELOW_THRESHOLD)

    override suspend fun addAboveThreshold(): Boolean = add(DemoScenarioKind.ABOVE_THRESHOLD)

    /** The next refresh re-delivers the same source ID; the notification policy must ignore it. */
    override suspend fun replayLatestAsDuplicate(): Boolean = dao.getLatest() != null

    /** Clears events, cursors, and notification history of the isolated demo database. */
    override suspend fun reset() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    private suspend fun add(kind: DemoScenarioKind): Boolean = dao.addScenario(
        DemoScenarioEntity(
            id = "demo-${UUID.randomUUID()}",
            kind = kind.name,
            addedAtEpochMillis = clock.millis(),
        ),
    ) != -1L
}
