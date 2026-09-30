package com.mlhysrszn.earthquake.data.local.demo

import androidx.room.Database
import androidx.room.RoomDatabase

/** Demo-only scenario storage, so the live database schema carries no demo tables. */
@Database(
    entities = [DemoScenarioEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DemoScenarioDatabase : RoomDatabase() {
    abstract fun demoScenarioDao(): DemoScenarioDao
}
