package com.mlhysrszn.earthquake.data.repository

import android.database.sqlite.SQLiteException
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.SnapshotWriteResult
import com.mlhysrszn.earthquake.data.local.room.toDomain
import com.mlhysrszn.earthquake.data.local.room.toEntity
import com.mlhysrszn.earthquake.data.remote.usgs.MalformedUsgsFeedException
import com.mlhysrszn.earthquake.data.remote.usgs.UsgsEarthquakeService
import com.mlhysrszn.earthquake.data.remote.usgs.UsgsGeoJsonMapper
import com.mlhysrszn.earthquake.domain.model.Earthquake
import com.mlhysrszn.earthquake.domain.repository.EarthquakeLookupResult
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.RefreshResult
import java.io.IOException
import java.time.Clock
import java.time.Instant
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
        earthquakeDao.observeRecent(clock::millis).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override fun observeLastSuccessfulRefresh(): Flow<Instant?> =
        earthquakeDao.observeSyncMetadata().map { metadata ->
            metadata?.lastSuccessfulFetchAtEpochMillis?.let(Instant::ofEpochMilli)
        }

    override fun observeEarthquake(id: String): Flow<Earthquake?> =
        earthquakeDao.observeEarthquake(id).map { entity -> entity?.toDomain() }

    override suspend fun refresh(): RefreshResult = try {
        refreshMutex.withLock {
            val feed = usgsService.fetchAllDaySummary().use { responseBody ->
                mapper.decode(responseBody.string())
            }
            val fetchedAt = clock.instant()
            val fetchedAtEpochMillis = fetchedAt.toEpochMilli()

            val writeResult = earthquakeDao.applySnapshot(
                earthquakes = feed.earthquakes.map(Earthquake::toEntity),
                sourceGeneratedAtEpochMillis = feed.sourceGeneratedAt?.toEpochMilli(),
                fetchedAtEpochMillis = fetchedAtEpochMillis,
                nowEpochMillis = fetchedAtEpochMillis,
            )

            // A stale snapshot is a valid response that replaces nothing, so it accepts no events.
            val acceptedEventCount = when (writeResult) {
                is SnapshotWriteResult.Applied -> writeResult.storedEventCount
                is SnapshotWriteResult.IgnoredStale -> 0
            }
            RefreshResult.Success(acceptedEventCount = acceptedEventCount)
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

    override suspend fun fetchEarthquakeById(id: String): EarthquakeLookupResult = try {
        refreshMutex.withLock {
            earthquakeDao.findEarthquake(id)?.let { cached ->
                return@withLock EarthquakeLookupResult.Found(cached.toDomain())
            }

            val earthquake = usgsService.fetchEventById(id).use { responseBody ->
                mapper.decodeSingleEvent(responseBody.string())
            } ?: return@withLock EarthquakeLookupResult.Unavailable

            if (earthquake.id != id) {
                return@withLock EarthquakeLookupResult.Failure(
                    RefreshResult.Reason.INVALID_RESPONSE,
                )
            }

            earthquakeDao.upsertDetailIfRecent(
                earthquake = earthquake.toEntity(),
                nowEpochMillis = clock.millis(),
            )
            EarthquakeLookupResult.Found(earthquake)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: HttpException) {
        if (exception.code() == HTTP_NOT_FOUND) {
            EarthquakeLookupResult.Unavailable
        } else {
            EarthquakeLookupResult.Failure(RefreshResult.Reason.NETWORK)
        }
    } catch (_: IOException) {
        EarthquakeLookupResult.Failure(RefreshResult.Reason.NETWORK)
    } catch (_: MalformedUsgsFeedException) {
        EarthquakeLookupResult.Failure(RefreshResult.Reason.INVALID_RESPONSE)
    } catch (_: SQLiteException) {
        EarthquakeLookupResult.Failure(RefreshResult.Reason.STORAGE)
    } catch (_: Exception) {
        EarthquakeLookupResult.Failure(RefreshResult.Reason.UNKNOWN)
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
