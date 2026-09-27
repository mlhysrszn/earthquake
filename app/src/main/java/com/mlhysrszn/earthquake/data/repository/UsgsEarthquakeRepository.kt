package com.mlhysrszn.earthquake.data.repository

import android.database.sqlite.SQLiteException
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.toDomain
import com.mlhysrszn.earthquake.data.local.room.toEntity
import com.mlhysrszn.earthquake.data.remote.usgs.MalformedUsgsFeedException
import com.mlhysrszn.earthquake.data.remote.usgs.UsgsEarthquakeService
import com.mlhysrszn.earthquake.data.remote.usgs.UsgsGeoJsonMapper
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsgsEarthquakeRepository @Inject constructor(
    private val earthquakeDao: EarthquakeDao,
    private val usgsService: UsgsEarthquakeService,
    private val mapper: UsgsGeoJsonMapper,
    private val clock: Clock,
) : EarthquakeRepository {
    private val refreshMutex = Mutex()

    override fun observeEarthquakes(): Flow<List<Earthquake>> =
        earthquakeDao.observeRecent(clock.millis()).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override suspend fun refresh(): RefreshResult = try {
        refreshMutex.withLock {
            val feed = usgsService.fetchAllDaySummary().use { responseBody ->
                mapper.decode(responseBody.string())
            }
            val fetchedAt = clock.instant()
            val fetchedAtEpochMillis = fetchedAt.toEpochMilli()

            earthquakeDao.applySnapshot(
                earthquakes = feed.earthquakes.map(Earthquake::toEntity),
                sourceGeneratedAtEpochMillis = feed.sourceGeneratedAt?.toEpochMilli(),
                fetchedAtEpochMillis = fetchedAtEpochMillis,
                nowEpochMillis = fetchedAtEpochMillis,
            )

            RefreshResult.Success(acceptedEventCount = feed.earthquakes.size)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: IOException) {
        RefreshResult.Failure(RefreshResult.Reason.NETWORK)
    } catch (_: HttpException) {
        RefreshResult.Failure(RefreshResult.Reason.NETWORK)
    } catch (_: MalformedUsgsFeedException) {
        RefreshResult.Failure(RefreshResult.Reason.INVALID_RESPONSE)
    } catch (_: SQLiteException) {
        RefreshResult.Failure(RefreshResult.Reason.STORAGE)
    } catch (_: Exception) {
        RefreshResult.Failure(RefreshResult.Reason.UNKNOWN)
    }
}
