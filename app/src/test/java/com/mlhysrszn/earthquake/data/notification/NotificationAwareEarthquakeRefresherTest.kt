package com.mlhysrszn.earthquake.data.notification

import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.ProductEventRecorder
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import com.mlhysrszn.earthquake.domain.model.ProductEventName
import com.mlhysrszn.earthquake.domain.model.RefreshOrigin
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
        val recordedEvents = mutableListOf<ProductEventName>()
        val success = RefreshResult.Success(acceptedEventCount = 2)
        val refresher = NotificationAwareEarthquakeRefresher(
            earthquakeRepository = FakeEarthquakeRepository { success },
            dispatchPendingNotifications = DispatchPendingNotifications {
                dispatchCount++
                1
            },
            productEventRecorder = ProductEventRecorder { name, _ -> recordedEvents += name },
        )

        assertEquals(success, refresher.refresh(RefreshOrigin.FOREGROUND))
        assertEquals(1, dispatchCount)
        assertEquals(
            listOf(ProductEventName.REFRESH_STARTED, ProductEventName.REFRESH_SUCCEEDED),
            recordedEvents,
        )
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
            productEventRecorder = ProductEventRecorder { _, _ -> },
        )

        assertEquals(failure, refresher.refresh(RefreshOrigin.FOREGROUND))
        assertFalse(dispatched)
    }

    @Test
    fun dispatcherFailureBecomesRetryableUnknownRefreshFailure() = runTest {
        val recordedEvents = mutableListOf<ProductEventName>()
        val refresher = NotificationAwareEarthquakeRefresher(
            earthquakeRepository = FakeEarthquakeRepository {
                RefreshResult.Success(acceptedEventCount = 0)
            },
            dispatchPendingNotifications = DispatchPendingNotifications { throw IOException() },
            productEventRecorder = ProductEventRecorder { name, _ -> recordedEvents += name },
        )

        assertEquals(
            RefreshResult.Failure(RefreshResult.Reason.UNKNOWN),
            refresher.refresh(RefreshOrigin.BACKGROUND),
        )
        assertEquals(
            listOf(
                ProductEventName.REFRESH_STARTED,
                ProductEventName.REFRESH_SUCCEEDED,
                ProductEventName.NOTIFICATION_PROCESSING_FAILED,
            ),
            recordedEvents,
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
            productEventRecorder = ProductEventRecorder { _, _ -> },
        )

        refresher.refresh(RefreshOrigin.BACKGROUND)
        refresher.refresh(RefreshOrigin.BACKGROUND)

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
