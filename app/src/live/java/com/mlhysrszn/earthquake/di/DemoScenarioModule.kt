package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.domain.model.DemoScenarioStatus
import com.mlhysrszn.earthquake.domain.repository.DemoScenarioController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

/** Live builds expose no scenario controls, so demo data can never enter live storage. */
@Module
@InstallIn(SingletonComponent::class)
object DemoScenarioModule {
    @Provides
    @Singleton
    fun provideDemoScenarioController(): DemoScenarioController = object : DemoScenarioController {
        override fun observeStatus() = flowOf(DemoScenarioStatus(isAvailable = false))

        override suspend fun addBelowThreshold() = false

        override suspend fun addAboveThreshold() = false

        override suspend fun replayLatestAsDuplicate() = false

        override suspend fun reset() = Unit
    }
}
