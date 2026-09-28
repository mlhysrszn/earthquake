package com.mlhysrszn.earthquake.ui.earthquakes.detail

import androidx.lifecycle.ViewModelStore
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.ui.earthquakes.list.MainDispatcherRule
import java.time.Instant
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EarthquakeDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cached event is shown without making a remote detail request`() =
        runTest(mainDispatcherRule.dispatcher) {
            val earthquake = sample()
            val fixture = fixture(
                eventId = earthquake.id,
                initialEvents = listOf(earthquake),
            )
            try {
                val state = fixture.viewModel.uiState.value

                assertEquals(EarthquakeDetailUiState.Content(earthquake), state)
                assertEquals(0, fixture.repository.remoteCallCount)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `missing event shows explicit unavailable state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = fixture(eventId = "removed-event", initialEvents = emptyList())
            try {
                assertEquals(
                    EarthquakeDetailUiState.Unavailable,
                    fixture.viewModel.uiState.value,
                )
                assertEquals(1, fixture.repository.remoteCallCount)
            } finally {
                fixture.clear()
            }
        }

    @Test
    fun `detail failure is state and retry can load the requested event`() =
        runTest(mainDispatcherRule.dispatcher) {
            val eventId = "requested-event"
            val fixture = fixture(
                eventId = eventId,
                initialEvents = emptyList(),
                initialResult = EarthquakeLookupResult.Failure(RefreshResult.Reason.NETWORK),
            )
            try {
                assertEquals(
                    EarthquakeDetailUiState.Failure(RefreshResult.Reason.NETWORK),
                    fixture.viewModel.uiState.value,
                )

                val expected = sample(id = eventId)
                fixture.repository.nextResult = EarthquakeLookupResult.Found(expected)
                fixture.viewModel.retry()
                advanceUntilIdle()

                assertEquals(EarthquakeDetailUiState.Content(expected), fixture.viewModel.uiState.value)
                assertEquals(2, fixture.repository.remoteCallCount)
            } finally {
                fixture.clear()
            }
        }

    private fun TestScope.fixture(
        eventId: String,
        initialEvents: List<Earthquake>,
        initialResult: EarthquakeLookupResult = EarthquakeLookupResult.Unavailable,
    ): Fixture {
        val repository = FakeEarthquakeRepository(initialEvents, initialResult)
        val viewModel = EarthquakeDetailViewModel(eventId, repository)
        val viewModelStore = ViewModelStore().apply { put("detail-test", viewModel) }
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        runCurrent()
        return Fixture(repository, viewModel, viewModelStore, collector)
    }

    private fun sample(id: String = "sample-002") = Earthquake(
        id = id,
        magnitude = 3.4,
        magnitudeType = "ml",
        place = "Central Alaska",
        occurredAt = Instant.parse("2026-09-28T11:36:00Z"),
        updatedAt = null,
        longitude = -150.1,
        latitude = 63.2,
        depthKm = 35.0,
        sourceUrl = null,
    )

    private data class Fixture(
        val repository: FakeEarthquakeRepository,
        val viewModel: EarthquakeDetailViewModel,
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
        initialResult: EarthquakeLookupResult,
    ) : EarthquakeRepository {
        private val events = MutableStateFlow(initialEvents)
        var nextResult = initialResult
        var remoteCallCount = 0
            private set

        override fun observeEarthquakes(): Flow<List<Earthquake>> = events

        override fun observeEarthquake(id: String): Flow<Earthquake?> =
            events.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun refresh(): RefreshResult =
            RefreshResult.Success(acceptedEventCount = events.value.size)

        override suspend fun fetchEarthquakeById(id: String): EarthquakeLookupResult {
            events.value.firstOrNull { it.id == id }?.let { return EarthquakeLookupResult.Found(it) }
            remoteCallCount++
            val result = nextResult
            if (result is EarthquakeLookupResult.Found) {
                events.value = events.value + result.earthquake
            }
            return result
        }
    }
}
