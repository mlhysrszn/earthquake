package com.mlhysrszn.earthquake.data.repository

import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory deterministic source for the first runnable list slice. */
@Singleton
class SampleEarthquakeRepository @Inject constructor(
    private val clock: Clock,
) : EarthquakeRepository {
    private val mutex = Mutex()
    private val samples = createSamples()
    private val mutableEarthquakes = MutableStateFlow(samples)
    private var nextRefreshGate: CompletableDeferred<Unit>? = null
    private var nextFailure: RefreshResult.Reason? = null

    private val earthquakes = mutableEarthquakes.asStateFlow()

    override fun observeEarthquakes(): Flow<List<Earthquake>> = earthquakes

    override suspend fun refresh(): RefreshResult {
        val gate = mutex.withLock {
            nextRefreshGate.also { nextRefreshGate = null }
        }
        gate?.await()

        return mutex.withLock {
            val failure = nextFailure.also { nextFailure = null }
            failure?.let(RefreshResult::Failure)
                ?: RefreshResult.Success(acceptedEventCount = mutableEarthquakes.value.size)
        }
    }

    /** Replaces the observed list with a deterministic empty result. */
    internal suspend fun showEmpty() {
        mutex.withLock { mutableEarthquakes.value = emptyList() }
    }

    /** Makes the next refresh fail once; a later retry succeeds normally. */
    internal suspend fun failNextRefresh(
        reason: RefreshResult.Reason = RefreshResult.Reason.UNKNOWN,
    ) {
        mutex.withLock { nextFailure = reason }
    }

    /** Holds the next refresh until the returned handle is released. */
    internal suspend fun holdNextRefresh(): SampleRefreshGate = mutex.withLock {
        check(nextRefreshGate == null) { "A refresh is already being held" }
        CompletableDeferred<Unit>().also { nextRefreshGate = it }.let(::SampleRefreshGate)
    }

    /** Restores the fixed sample set and clears pending scenario controls. */
    internal suspend fun reset() {
        val gate = mutex.withLock {
            mutableEarthquakes.value = samples
            nextFailure = null
            nextRefreshGate.also { nextRefreshGate = null }
        }
        gate?.complete(Unit)
    }

    private fun createSamples(): List<Earthquake> {
        val now = clock.instant()
        return listOf(
            sample(
                id = "sample-001",
                magnitude = 5.1,
                magnitudeType = "mw",
                place = "Near the coast of Northern California",
                occurredAt = now.minus(Duration.ofMinutes(7)),
                longitude = -124.2,
                latitude = 40.3,
                depthKm = 12.0,
            ),
            sample(
                id = "sample-002",
                magnitude = 3.4,
                magnitudeType = "ml",
                place = "Central Alaska",
                occurredAt = now.minus(Duration.ofMinutes(24)),
                longitude = -150.1,
                latitude = 63.2,
                depthKm = 35.0,
            ),
            sample(
                id = "sample-003",
                magnitude = 2.1,
                magnitudeType = "md",
                place = "Western Texas",
                occurredAt = now.minus(Duration.ofMinutes(51)),
                longitude = -103.7,
                latitude = 31.8,
                depthKm = 6.0,
            ),
        )
    }

    private fun sample(
        id: String,
        magnitude: Double,
        magnitudeType: String,
        place: String,
        occurredAt: Instant,
        longitude: Double,
        latitude: Double,
        depthKm: Double,
    ) = Earthquake(
        id = id,
        magnitude = magnitude,
        magnitudeType = magnitudeType,
        place = place,
        occurredAt = occurredAt,
        updatedAt = occurredAt.plusSeconds(30),
        longitude = longitude,
        latitude = latitude,
        depthKm = depthKm,
        sourceUrl = "https://example.test/earthquakes/$id",
    )
}

/** Test control for a refresh deliberately held in progress. */
internal class SampleRefreshGate internal constructor(
    private val releaseSignal: CompletableDeferred<Unit>,
) {
    fun release() {
        releaseSignal.complete(Unit)
    }
}
