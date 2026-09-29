package com.mlhysrszn.earthquake.domain.repository

import com.mlhysrszn.earthquake.domain.model.DemoScenarioStatus
import kotlinx.coroutines.flow.Flow

/** Variant-selected scenario controls; live builds provide an unavailable no-op. */
interface DemoScenarioController {
    fun observeStatus(): Flow<DemoScenarioStatus>

    suspend fun addBelowThreshold(): Boolean

    suspend fun addAboveThreshold(): Boolean

    suspend fun replayLatestAsDuplicate(): Boolean

    suspend fun reset()
}
