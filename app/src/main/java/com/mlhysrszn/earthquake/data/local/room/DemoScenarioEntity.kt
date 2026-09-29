package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "demo_scenario_events")
data class DemoScenarioEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val addedAtEpochMillis: Long,
)
