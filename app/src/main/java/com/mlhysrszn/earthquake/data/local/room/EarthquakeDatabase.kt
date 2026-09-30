package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        EarthquakeEntity::class,
        SyncMetadataEntity::class,
        NotificationProcessingEntity::class,
        NotificationEventAliasEntity::class,
        NotificationProcessingMetadataEntity::class,
        ProductEventEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class EarthquakeDatabase : RoomDatabase() {
    abstract fun earthquakeDao(): EarthquakeDao

    abstract fun notificationProcessingDao(): NotificationProcessingDao

    abstract fun productEventDao(): ProductEventDao

}
