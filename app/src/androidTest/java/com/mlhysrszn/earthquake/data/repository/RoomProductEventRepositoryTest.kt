package com.mlhysrszn.earthquake.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase
import com.mlhysrszn.earthquake.domain.model.ProductEvent
import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomProductEventRepositoryTest {
    @Test
    fun recordsUtcTimestampMinimalPropertiesAndDemoEnvironmentForInspection() =
        runBlocking { withRepository { repository ->
            val event = ProductEvent(
                id = "demo-event-1",
                name = ProductEventName.NOTIFICATION_POSTED,
                occurredAt = Instant.parse("2026-09-29T12:34:56Z"),
                environment = ProductEventEnvironment.DEMO,
                properties = mapOf("event_id" to "sample-001", "magnitude" to "5.1"),
            )

            repository.record(event)

            assertEquals(event, repository.getRecentEvents().single())
        } }

    @Test
    fun historyPrunesRowsOutsideRetentionAndReturnsNewestEventsFirst() = runBlocking {
        withRepository { repository ->
        val now = Instant.parse("2026-09-29T12:00:00Z")
        repository.record(
            ProductEvent(
                id = "expired",
                name = ProductEventName.SCREEN_VIEW,
                occurredAt = now.minusSeconds(91L * 24 * 60 * 60),
                environment = ProductEventEnvironment.LIVE,
                properties = mapOf("screen" to "list"),
            ),
        )
        val newest = ProductEvent(
            id = "newest",
            name = ProductEventName.REFRESH_FAILED,
            occurredAt = now,
            environment = ProductEventEnvironment.LIVE,
            properties = mapOf("origin" to "foreground", "reason" to "network"),
        )
        val older = newest.copy(
            id = "older",
            occurredAt = now.minusSeconds(10),
        )
        repository.record(older)
        repository.record(newest)

        assertEquals(listOf(newest, older), repository.getRecentEvents())
        }
    }

    private suspend fun withRepository(test: suspend (RoomProductEventRepository) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            EarthquakeDatabase::class.java,
        ).build()
        try {
            test(RoomProductEventRepository(database.productEventDao()))
        } finally {
            database.close()
        }
    }
}
