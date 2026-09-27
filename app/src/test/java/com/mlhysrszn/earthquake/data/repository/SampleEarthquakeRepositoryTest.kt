package com.mlhysrszn.earthquake.data.repository

import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleEarthquakeRepositoryTest {
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-09-28T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `observes fixed events newest first`() = runBlocking {
        val repository = SampleEarthquakeRepository(fixedClock)

        val firstRead = repository.observeEarthquakes().first()
        val secondRead = repository.observeEarthquakes().first()

        assertEquals(
            listOf("sample-001", "sample-002", "sample-003"),
            firstRead.map { it.id },
        )
        assertEquals(firstRead, secondRead)
        assertEquals(Instant.parse("2026-09-28T11:53:00Z"), firstRead.first().occurredAt)
    }

    @Test
    fun `empty scenario emits no events and reset restores the samples`() = runBlocking {
        val repository = SampleEarthquakeRepository(fixedClock)
        val expected = repository.observeEarthquakes().first()

        repository.showEmpty()
        assertTrue(repository.observeEarthquakes().first().isEmpty())
        assertEquals(RefreshResult.Success(acceptedEventCount = 0), repository.refresh())

        repository.reset()
        assertEquals(expected, repository.observeEarthquakes().first())
    }

    @Test
    fun `failed refresh preserves content and the following retry succeeds`() = runBlocking {
        val repository = SampleEarthquakeRepository(fixedClock)
        val beforeRefresh = repository.observeEarthquakes().first()
        repository.failNextRefresh(RefreshResult.Reason.NETWORK)

        assertEquals(
            RefreshResult.Failure(RefreshResult.Reason.NETWORK),
            repository.refresh(),
        )
        assertEquals(beforeRefresh, repository.observeEarthquakes().first())
        assertEquals(RefreshResult.Success(acceptedEventCount = 3), repository.refresh())
    }

    @Test
    fun `held refresh remains loading until explicitly released`() = runBlocking {
        val repository = SampleEarthquakeRepository(fixedClock)
        val gate = repository.holdNextRefresh()
        val refresh = async(start = CoroutineStart.UNDISPATCHED) { repository.refresh() }

        assertFalse(refresh.isCompleted)
        gate.release()

        assertEquals(RefreshResult.Success(acceptedEventCount = 3), refresh.await())
    }
}
