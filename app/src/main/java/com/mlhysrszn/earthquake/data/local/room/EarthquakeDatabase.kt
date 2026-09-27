package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EarthquakeEntity::class, SyncMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class EarthquakeDatabase : RoomDatabase() {
    abstract fun earthquakeDao(): EarthquakeDao
}
