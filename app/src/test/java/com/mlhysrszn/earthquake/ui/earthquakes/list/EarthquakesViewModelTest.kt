package com.mlhysrszn.earthquake.ui.earthquakes.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EarthquakesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-09-28T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `initial success exposes observed content and successful update time`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = fixture(initialEvents = listOf(sample()))
            try {
                val state = fixture.viewModel.uiState.value

                assertEquals(listOf("sample-001"), state.earthquakes.map { it.id })
                assertFalse(state.isInitialLoading)
                assertFalse(state.isRefreshing)
                assertNull(state.refreshError)
                assertEquals(fixedClock.instant(), state.lastUpdatedAt)

                val updated = sample(id = "sample-002")
                fixture.repository.events.value = listOf(updated)
                runCurrent()
                assertEquals(listOf(updated), fixture.viewModel.uiState.value.earthquakes)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `empty success exposes a completed empty state`() = runTest(mainDispatcherRule.dispatcher) {
        val fixture = fixture(initialEvents = emptyList())
        try {
            val state = fixture.viewModel.uiState.value

            assertTrue(state.earthquakes.isEmpty())
            assertFalse(state.isInitialLoading)
            assertNull(state.refreshError)
            assertEquals(fixedClock.instant(), state.lastUpdatedAt)
        } finally {
            fixture.clear()
        }
    }

    @Test
    fun `initial failure is represented in state and retry clears it`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = fixture(
                initialEvents = emptyList(),
                initialResult = RefreshResult.Failure(RefreshResult.Reason.NETWORK),
            )
            try {
                val failedState = fixture.viewModel.uiState.value
                assertTrue(failedState.earthquakes.isEmpty())
                assertFalse(failedState.isInitialLoading)
                assertEquals(RefreshResult.Reason.NETWORK, failedState.refreshError)
                assertNull(failedState.lastUpdatedAt)

                fixture.repository.enqueue(RefreshResult.Success(acceptedEventCount = 0))
                fixture.viewModel.retry()
                advanceUntilIdle()

                val retriedState = fixture.viewModel.uiState.value
                assertFalse(retriedState.isRefreshing)
                assertNull(retriedState.refreshError)
                assertEquals(fixedClock.instant(), retriedState.lastUpdatedAt)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `failed refresh retains existing content and prior update time`() =
        runTest(mainDispatcherRule.dispatcher) {
            val initialEvent = sample()
            val fixture = fixture(initialEvents = listOf(initialEvent))
            try {
                val lastUpdatedAt = fixture.viewModel.uiState.value.lastUpdatedAt
                fixture.repository.enqueue(RefreshResult.Failure(RefreshResult.Reason.STORAGE))

                fixture.viewModel.refresh()
                advanceUntilIdle()

                val state = fixture.viewModel.uiState.value
                assertEquals(listOf(initialEvent), state.earthquakes)
                assertEquals(RefreshResult.Reason.STORAGE, state.refreshError)
                assertEquals(lastUpdatedAt, state.lastUpdatedAt)
                assertFalse(state.isRefreshing)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `manual refresh remains active until repository completes`() =
        runTest(mainDispatcherRule.dispatcher) {
            val initialEvent = sample()
            val fixture = fixture(initialEvents = listOf(initialEvent))
            try {
                val pendingResult = CompletableDeferred<RefreshResult>()
                fixture.repository.nextRefreshGate = pendingResult

                fixture.viewModel.refresh()
                runCurrent()

                val loadingState = fixture.viewModel.uiState.value
                assertTrue(loadingState.isRefreshing)
                assertFalse(loadingState.isInitialLoading)
                assertEquals(listOf(initialEvent), loadingState.earthquakes)

                pendingResult.complete(RefreshResult.Success(acceptedEventCount = 1))
                advanceUntilIdle()

                assertFalse(fixture.viewModel.uiState.value.isRefreshing)
                assertNull(fixture.viewModel.uiState.value.refreshError)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `persisted last update time is shown before and after a failed refresh`() =
        runTest(mainDispatcherRule.dispatcher) {
            val persisted = Instant.parse("2026-09-28T09:00:00Z")
            val fixture = fixture(
                initialEvents = listOf(sample()),
                initialResult = RefreshResult.Failure(RefreshResult.Reason.NETWORK),
                persistedLastRefresh = persisted,
            )
            try {
                val state = fixture.viewModel.uiState.value
                assertEquals(RefreshResult.Reason.NETWORK, state.refreshError)
                assertEquals(persisted, state.lastUpdatedAt)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `resume refreshes only when data is older than the stale limit`() =
        runTest(mainDispatcherRule.dispatcher) {
            val clock = MutableClock(fixedClock.instant())
            val fixture = fixture(initialEvents = listOf(sample()), clock = clock)
            try {
                assertEquals(1, fixture.repository.refreshCount)

                clock.now = clock.now.plus(EarthquakesViewModel.STALE_AFTER).minusSeconds(1)
                fixture.viewModel.refreshIfStale()
                advanceUntilIdle()
                assertEquals(1, fixture.repository.refreshCount)

                clock.now = clock.now.plusSeconds(1)
                fixture.viewModel.refreshIfStale()
                advanceUntilIdle()
                assertEquals(2, fixture.repository.refreshCount)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `magnitude filter hides smaller and unknown magnitudes but keeps the total`() =
        runTest(mainDispatcherRule.dispatcher) {
            val strong = sample(id = "strong", magnitude = 5.2)
            val weak = sample(id = "weak", magnitude = 2.4)
            val unknown = sample(id = "unknown", magnitude = null)
            val fixture = fixture(initialEvents = listOf(strong, weak, unknown))
            try {
                fixture.viewModel.setMinimumMagnitude(4.0)
                runCurrent()
                val filtered = fixture.viewModel.uiState.value
                assertEquals(listOf("strong"), filtered.earthquakes.map { it.id })
                assertEquals(3, filtered.totalEarthquakeCount)
                assertEquals(4.0, filtered.minimumMagnitude!!, 0.0)

                fixture.viewModel.setMinimumMagnitude(null)
                runCurrent()
                assertEquals(3, fixture.viewModel.uiState.value.earthquakes.size)
            } finally {
                fixture.clear()
            }
        }

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    private fun TestScope.fixture(
        initialEvents: List<Earthquake>,
        initialResult: RefreshResult = RefreshResult.Success(initialEvents.size),
        persistedLastRefresh: Instant? = null,
        clock: Clock = fixedClock,
    ): Fixture {
        val repository = FakeEarthquakeRepository(initialEvents, clock).apply {
            lastSuccessfulRefresh.value = persistedLastRefresh
            enqueue(initialResult)
        }
        val viewModel = EarthquakesViewModel(
            repository = repository,
            refreshEarthquakes = RefreshEarthquakes { _ -> repository.refresh() },
            clock = clock,
            savedStateHandle = SavedStateHandle(),
        )
        val viewModelStore = ViewModelStore().apply { put("test", viewModel) }
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        runCurrent()
        return Fixture(repository, viewModel, viewModelStore, collector)
    }

    private fun sample(id: String = "sample-001", magnitude: Double? = 4.2) = Earthquake(
        id = id,
        magnitude = magnitude,
        magnitudeType = "ml",
        place = "Near the coast",
        occurredAt = Instant.parse("2026-09-28T11:00:00Z"),
        updatedAt = null,
        longitude = -122.0,
        latitude = 37.0,
        depthKm = 8.0,
        sourceUrl = null,
    )

    private data class Fixture(
        val repository: FakeEarthquakeRepository,
        val viewModel: EarthquakesViewModel,
        private val viewModelStore: ViewModelStore,
        private val collector: Job,
    ) {
        fun clear() {
            collector.cancel()
            viewModelStore.clear()
        }
    }

    private class FakeEarthquakeRepository(
        initialEvents: List<Earthquake>,
        private val clock: Clock,
    ) : EarthquakeRepository {
        val events = MutableStateFlow(initialEvents)
        val lastSuccessfulRefresh = MutableStateFlow<Instant?>(null)
        var refreshCount = 0
            private set
        var nextRefreshGate: CompletableDeferred<RefreshResult>? = null
        private val queuedResults = ArrayDeque<RefreshResult>()

        override fun observeEarthquakes(): Flow<List<Earthquake>> = events

        override fun observeLastSuccessfulRefresh(): Flow<Instant?> = lastSuccessfulRefresh

        override fun observeEarthquake(id: String): Flow<Earthquake?> =
            events.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun refresh(): RefreshResult {
            refreshCount++
            val gate = nextRefreshGate.also { nextRefreshGate = null }
            val result = gate?.await() ?: queuedResults.removeFirstOrNull()
                ?: RefreshResult.Success(acceptedEventCount = events.value.size)
            if (result is RefreshResult.Success) lastSuccessfulRefresh.value = clock.instant()
            return result
        }

        fun enqueue(result: RefreshResult) {
            queuedResults.addLast(result)
        }

        override suspend fun fetchEarthquakeById(id: String): EarthquakeLookupResult =
            events.value.firstOrNull { it.id == id }
                ?.let(EarthquakeLookupResult::Found)
                ?: EarthquakeLookupResult.Unavailable
    }
}
