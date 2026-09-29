package com.mlhysrszn.earthquake.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DemoScenarioDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addScenario(event: DemoScenarioEntity): Long

    @Query("SELECT * FROM demo_scenario_events ORDER BY addedAtEpochMillis ASC, id ASC")
    suspend fun getAll(): List<DemoScenarioEntity>

    @Query("SELECT * FROM demo_scenario_events ORDER BY addedAtEpochMillis ASC, id ASC")
    fun observeAll(): Flow<List<DemoScenarioEntity>>

    @Query("SELECT * FROM demo_scenario_events ORDER BY addedAtEpochMillis DESC, id DESC LIMIT 1")
    suspend fun getLatest(): DemoScenarioEntity?

    @Query("DELETE FROM demo_scenario_events")
    suspend fun clear()
}
