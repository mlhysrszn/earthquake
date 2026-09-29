package com.mlhysrszn.earthquake.data.notification

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabaseMigrations
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase
import com.mlhysrszn.earthquake.data.local.room.EarthquakeEntity
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingDao
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingEntity
import com.mlhysrszn.earthquake.data.local.room.NotificationProcessingOutcome
import com.mlhysrszn.earthquake.data.local.room.SnapshotWriteResult
import com.mlhysrszn.earthquake.data.local.room.toEntity
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.model.NotificationPreferences
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.usecase.NotificationEligibilityPolicy
import com.mlhysrszn.earthquake.domain.usecase.NotificationSnapshotStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationProcessorTest {
    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val nowMillis = now.toEpochMilli()
    private val sourceCursorStart = nowMillis - 10 * 60_000L
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun firstSnapshotEstablishesBaselineAndOnlyLaterNewEventsBecomePending() = runBlocking {
        withInMemoryDatabase { earthquakeDao, processingDao ->
            val processor = processor(earthquakeDao, processingDao)
            val historic = event("historic-event", magnitude = 7.2)
            storeSnapshot(earthquakeDao, listOf(historic), sourceCursorStart)

            val baseline = processor.processLatestSnapshot(notificationPermissionGranted = true)

            assertEquals(NotificationSnapshotStatus.INITIAL_BASELINE, baseline.snapshotStatus)
            assertTrue(baseline.pendingNotifications.isEmpty())
            assertTrue(processingDao.getMetadata()?.baselineEstablished == true)

            val candidate = event("new-event", magnitude = 5.0, occurredAt = now.minusSeconds(30))
            storeSnapshot(earthquakeDao, listOf(historic, candidate), sourceCursorStart + 60_000)

            val fresh = processor.processLatestSnapshot(notificationPermissionGranted = true)

            assertEquals(NotificationSnapshotStatus.FRESH, fresh.snapshotStatus)
            assertEquals(listOf("new-event"), fresh.pendingNotifications.map { it.eventId })
            val states = processingDao.getAllEventStates().associateBy(
                NotificationProcessingEntity::canonicalEventId,
            )
            assertEquals(NotificationProcessingOutcome.BASELINED.name, states.getValue("historic-event").outcome)
            assertEquals(NotificationProcessingOutcome.PENDING.name, states.getValue("new-event").outcome)

            val duplicateRun = processor.processLatestSnapshot(notificationPermissionGranted = true)
            assertEquals(NotificationSnapshotStatus.STALE, duplicateRun.snapshotStatus)
            assertTrue(duplicateRun.pendingNotifications.isEmpty())

            assertTrue(processor.markPosted("new-event"))
            val afterDelivery = processor.processLatestSnapshot(notificationPermissionGranted = true)
            assertTrue(afterDelivery.pendingNotifications.isEmpty())
            assertEquals(
                NotificationProcessingOutcome.POSTED.name,
                processingDao.getEventState("new-event")?.outcome,
            )
        }
    }

    @Test
    fun processedIdentitiesAndPendingDeliveryRecoverAfterDatabaseReopen() = runBlocking {
        val databaseName = "notification-recovery-${UUID.randomUUID()}.db"
        val candidate = event("recoverable-event", magnitude = 5.5)
        try {
            val firstDatabase = openDatabase(databaseName)
            try {
                val earthquakeDao = firstDatabase.earthquakeDao()
                val processingDao = firstDatabase.notificationProcessingDao()
                val firstProcessor = processor(earthquakeDao, processingDao)
                storeSnapshot(earthquakeDao, emptyList(), sourceCursorStart)
                firstProcessor.processLatestSnapshot(notificationPermissionGranted = true)
                storeSnapshot(earthquakeDao, listOf(candidate), sourceCursorStart + 60_000)

                val pending = firstProcessor.processLatestSnapshot(notificationPermissionGranted = true)
                assertEquals(listOf("recoverable-event"), pending.pendingNotifications.map { it.eventId })
            } finally {
                firstDatabase.close()
            }

            val reopenedDatabase = openDatabase(databaseName)
            try {
                val earthquakeDao = reopenedDatabase.earthquakeDao()
                val processingDao = reopenedDatabase.notificationProcessingDao()
                val restartedProcessor = processor(earthquakeDao, processingDao)

                val recovered = restartedProcessor.processLatestSnapshot(notificationPermissionGranted = true)

                assertEquals(NotificationSnapshotStatus.STALE, recovered.snapshotStatus)
                assertEquals(listOf("recoverable-event"), recovered.pendingNotifications.map { it.eventId })
                assertTrue(restartedProcessor.markPosted("recoverable-event"))
            } finally {
                reopenedDatabase.close()
            }

            val finalDatabase = openDatabase(databaseName)
            try {
                val dao = finalDatabase.notificationProcessingDao()
                val freshProcessor = processor(finalDatabase.earthquakeDao(), dao)
                assertTrue(
                    freshProcessor.processLatestSnapshot(notificationPermissionGranted = true)
                        .pendingNotifications.isEmpty(),
                )
                assertEquals(
                    NotificationProcessingOutcome.POSTED.name,
                    dao.getEventState("recoverable-event")?.outcome,
                )
            } finally {
                finalDatabase.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun aliasesDeduplicateEventsAndConflictingAliasesAreQuarantined() = runBlocking {
        withInMemoryDatabase { earthquakeDao, processingDao ->
            val processor = processor(earthquakeDao, processingDao)
            val first = event("canonical-a", magnitude = 6.0, aliases = setOf("alias-a"))
            val second = event("canonical-b", magnitude = 5.5, aliases = setOf("alias-b"))
            storeSnapshot(earthquakeDao, listOf(first, second), sourceCursorStart)
            processor.processLatestSnapshot(notificationPermissionGranted = true)

            val renamed = event(
                "alias-a",
                magnitude = 8.0,
                aliases = setOf("canonical-a"),
                occurredAt = now.minusSeconds(20),
            )
            val ambiguous = event(
                "ambiguous-event",
                magnitude = 8.0,
                aliases = setOf("alias-a", "alias-b"),
                occurredAt = now.minusSeconds(10),
            )
            storeSnapshot(earthquakeDao, listOf(first, second, renamed, ambiguous), sourceCursorStart + 60_000)

            val result = processor.processLatestSnapshot(notificationPermissionGranted = true)

            assertTrue(result.pendingNotifications.isEmpty())
            val states = processingDao.getAllEventStates().associateBy(
                NotificationProcessingEntity::canonicalEventId,
            )
            assertEquals(3, states.size)
            assertEquals("alias-a", states.getValue("canonical-a").latestEventId)
            assertEquals(
                NotificationProcessingOutcome.AMBIGUOUS_IDENTITY.name,
                states.getValue("ambiguous-event").outcome,
            )
            assertEquals(
                "canonical-a",
                processingDao.getAllAliases().first { it.aliasId == "alias-a" }.canonicalEventId,
            )
        }
    }

    @Test
    fun disabledAndPermissionDeniedEventsAreTerminalAndDoNotReplay() = runBlocking {
        withInMemoryDatabase { earthquakeDao, processingDao ->
            val preferences = FakeNotificationPreferencesRepository(
                NotificationPreferences(notificationsEnabled = false, magnitudeThreshold = 4.0),
            )
            val processor = processor(earthquakeDao, processingDao, preferences)
            storeSnapshot(earthquakeDao, emptyList(), sourceCursorStart)
            processor.processLatestSnapshot(notificationPermissionGranted = true)

            val disabledEvent = event("disabled-event", magnitude = 6.0)
            storeSnapshot(earthquakeDao, listOf(disabledEvent), sourceCursorStart + 60_000)
            assertTrue(processor.processLatestSnapshot(notificationPermissionGranted = true)
                .pendingNotifications.isEmpty())
            assertEquals(
                NotificationProcessingOutcome.SUPPRESSED_DISABLED.name,
                processingDao.getEventState("disabled-event")?.outcome,
            )

            preferences.update(
                NotificationPreferences(notificationsEnabled = true, magnitudeThreshold = 4.0),
            )
            val deniedEvent = event("denied-event", magnitude = 6.0, occurredAt = now.minusSeconds(30))
            storeSnapshot(
                earthquakeDao,
                listOf(disabledEvent, deniedEvent),
                sourceCursorStart + 120_000,
            )
            assertTrue(processor.processLatestSnapshot(notificationPermissionGranted = false)
                .pendingNotifications.isEmpty())
            assertEquals(
                NotificationProcessingOutcome.SUPPRESSED_PERMISSION.name,
                processingDao.getEventState("denied-event")?.outcome,
            )

            storeSnapshot(
                earthquakeDao,
                listOf(disabledEvent, deniedEvent),
                sourceCursorStart + 180_000,
            )
            assertTrue(processor.processLatestSnapshot(notificationPermissionGranted = true)
                .pendingNotifications.isEmpty())
            assertEquals(
                NotificationProcessingOutcome.SUPPRESSED_PERMISSION.name,
                processingDao.getEventState("denied-event")?.outcome,
            )
        }
    }

    @Test
    fun retryableDeliveryCanBeRecoveredAndPermissionDenialBecomesTerminal() = runBlocking {
        withInMemoryDatabase { earthquakeDao, processingDao ->
            val processor = processor(earthquakeDao, processingDao)
            storeSnapshot(earthquakeDao, emptyList(), sourceCursorStart)
            processor.processLatestSnapshot(notificationPermissionGranted = true)
            storeSnapshot(
                earthquakeDao,
                listOf(event("retry-event", magnitude = 5.2)),
                sourceCursorStart + 60_000,
            )

            assertEquals(
                listOf("retry-event"),
                processor.processLatestSnapshot(notificationPermissionGranted = true)
                    .pendingNotifications.map { it.eventId },
            )
            assertTrue(processor.markRetryable("retry-event"))
            assertEquals(
                listOf("retry-event"),
                processor.processLatestSnapshot(notificationPermissionGranted = true)
                    .pendingNotifications.map { it.eventId },
            )

            assertTrue(processor.markPermissionDenied("retry-event"))
            assertTrue(
                processor.processLatestSnapshot(notificationPermissionGranted = true)
                    .pendingNotifications.isEmpty(),
            )
            assertEquals(
                NotificationProcessingOutcome.SUPPRESSED_PERMISSION.name,
                processingDao.getEventState("retry-event")?.outcome,
            )
        }
    }

    @Test
    fun concurrentProcessorsOnlyEmitOneInProcessCandidate() = runBlocking {
        withInMemoryDatabase { earthquakeDao, processingDao ->
            val processor = processor(earthquakeDao, processingDao)
            storeSnapshot(earthquakeDao, emptyList(), sourceCursorStart)
            processor.processLatestSnapshot(notificationPermissionGranted = true)
            storeSnapshot(
                earthquakeDao,
                listOf(event("concurrent-event", magnitude = 5.0)),
                sourceCursorStart + 60_000,
            )

            val first = async { processor.processLatestSnapshot(notificationPermissionGranted = true) }
            val second = async { processor.processLatestSnapshot(notificationPermissionGranted = true) }
            val pendingCount = first.await().pendingNotifications.size +
                second.await().pendingNotifications.size

            assertEquals(1, pendingCount)
            assertEquals(
                NotificationProcessingOutcome.PENDING.name,
                processingDao.getEventState("concurrent-event")?.outcome,
            )
        }
    }

    private fun processor(
        earthquakeDao: EarthquakeDao,
        processingDao: NotificationProcessingDao,
        preferences: FakeNotificationPreferencesRepository = FakeNotificationPreferencesRepository(
            NotificationPreferences(notificationsEnabled = true, magnitudeThreshold = 4.0),
        ),
    ) = NotificationProcessor(
        earthquakeDao = earthquakeDao,
        processingDao = processingDao,
        preferencesRepository = preferences,
        eligibilityPolicy = NotificationEligibilityPolicy(fixedClock),
        clock = fixedClock,
    )

    private suspend fun storeSnapshot(
        dao: EarthquakeDao,
        events: List<Earthquake>,
        generatedAtEpochMillis: Long,
    ) {
        val result = dao.applySnapshot(
            earthquakes = events.map(Earthquake::toEntity),
            sourceGeneratedAtEpochMillis = generatedAtEpochMillis,
            fetchedAtEpochMillis = nowMillis,
            nowEpochMillis = nowMillis,
        )
        assertTrue(result is SnapshotWriteResult.Applied)
    }

    private suspend fun withInMemoryDatabase(
        test: suspend (EarthquakeDao, NotificationProcessingDao) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            EarthquakeDatabase::class.java,
        ).build()
        try {
            test(database.earthquakeDao(), database.notificationProcessingDao())
        } finally {
            database.close()
        }
    }

    private fun openDatabase(name: String) = Room.databaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        EarthquakeDatabase::class.java,
        name,
    ).addMigrations(EarthquakeDatabaseMigrations.MIGRATION_1_2).build()

    private fun event(
        id: String,
        magnitude: Double,
        aliases: Set<String> = emptySet(),
        occurredAt: Instant = now.minusSeconds(60),
    ) = Earthquake(
        id = id,
        magnitude = magnitude,
        magnitudeType = "ml",
        place = "Near the coast",
        occurredAt = occurredAt,
        updatedAt = null,
        longitude = -122.0,
        latitude = 37.0,
        depthKm = 8.0,
        sourceUrl = null,
        aliases = aliases,
    )

    private class FakeNotificationPreferencesRepository(
        initial: NotificationPreferences,
    ) : NotificationPreferencesRepository {
        private val state = MutableStateFlow(initial)

        override fun observePreferences(): Flow<NotificationPreferences> = state.asStateFlow()

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            state.value = state.value.copy(notificationsEnabled = enabled)
        }

        override suspend fun setMagnitudeThreshold(threshold: Double) {
            state.value = state.value.copy(magnitudeThreshold = threshold)
        }

        fun update(preferences: NotificationPreferences) {
            state.value = preferences
        }
    }

    private companion object {
        val now = Instant.parse("2026-09-28T12:00:00Z")
        val nowMillis = now.toEpochMilli()
        val sourceCursorStart = nowMillis - 10 * 60_000L
    }
}
