package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.repository.RoomDemoScenarioController
import com.mlhysrszn.earthquake.domain.repository.DemoScenarioController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DemoScenarioModule {
    @Binds
    @Singleton
    abstract fun bindDemoScenarioController(
        implementation: RoomDemoScenarioController,
    ): DemoScenarioController
}
