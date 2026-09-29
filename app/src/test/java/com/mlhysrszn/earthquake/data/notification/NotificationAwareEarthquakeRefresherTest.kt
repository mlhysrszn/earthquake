package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.usecase.DispatchPendingNotifications
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationAwareEarthquakeRefresherTest {
    @Test
    fun successfulRefreshDispatchesPendingNotifications() = runTest {
        var dispatchCount = 0
        val success = RefreshResult.Success(acceptedEventCount = 2)
        val refresher = NotificationAwareEarthquakeRefresher(
            earthquakeRepository = FakeEarthquakeRepository { success },
            dispatchPendingNotifications = DispatchPendingNotifications {
                dispatchCount++
                1
            },
        )

        assertEquals(success, refresher.refresh())
        assertEquals(1, dispatchCount)
    }

    @Test
    fun failedRefreshDoesNotDispatchOrProcessAnUnacceptedSnapshot() = runTest {
        var dispatched = false
        val failure = RefreshResult.Failure(RefreshResult.Reason.NETWORK)
        val refresher = NotificationAwareEarthquakeRefresher(
            earthquakeRepository = FakeEarthquakeRepository { failure },
            dispatchPendingNotifications = DispatchPendingNotifications {
                dispatched = true
                0
            },
        )

        assertEquals(failure, refresher.refresh())
        assertFalse(dispatched)
    }

    @Test
    fun dispatcherFailureBecomesRetryableUnknownRefreshFailure() = runTest {
        val refresher = NotificationAwareEarthquakeRefresher(
            earthquakeRepository = FakeEarthquakeRepository {
                RefreshResult.Success(acceptedEventCount = 0)
            },
            dispatchPendingNotifications = DispatchPendingNotifications { throw IOException() },
        )

        assertEquals(
            RefreshResult.Failure(RefreshResult.Reason.UNKNOWN),
            refresher.refresh(),
        )
    }

    @Test
    fun eachSuccessfulRefreshReevaluatesPersistentPendingWork() = runTest {
        var dispatchCount = 0
        val refresher = NotificationAwareEarthquakeRefresher(
            earthquakeRepository = FakeEarthquakeRepository {
                RefreshResult.Success(acceptedEventCount = 0)
            },
            dispatchPendingNotifications = DispatchPendingNotifications {
                dispatchCount++
                0
            },
        )

        refresher.refresh()
        refresher.refresh()

        assertEquals(2, dispatchCount)
    }

    private class FakeEarthquakeRepository(
        private val refreshBlock: suspend () -> RefreshResult,
    ) : EarthquakeRepository {
        override fun observeEarthquakes(): Flow<List<com.mlhysrszn.earthquake.domain.model.Earthquake>> =
            emptyFlow()

        override fun observeEarthquake(
            id: String,
        ): Flow<com.mlhysrszn.earthquake.domain.model.Earthquake?> = emptyFlow()

        override suspend fun refresh(): RefreshResult = refreshBlock()

        override suspend fun fetchEarthquakeById(id: String): EarthquakeLookupResult =
            EarthquakeLookupResult.Unavailable
    }
}
