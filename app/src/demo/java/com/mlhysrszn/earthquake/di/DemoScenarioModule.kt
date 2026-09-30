package com.mlhysrszn.earthquake.di

import android.content.Context
import androidx.room.Room
import com.mlhysrszn.earthquake.data.local.demo.DemoScenarioDao
import com.mlhysrszn.earthquake.data.local.demo.DemoScenarioDatabase
import com.mlhysrszn.earthquake.data.repository.RoomDemoScenarioController
import com.mlhysrszn.earthquake.domain.repository.DemoScenarioController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    companion object {
        @Provides
        @Singleton
        fun provideDemoScenarioDatabase(
            @ApplicationContext context: Context,
        ): DemoScenarioDatabase = Room.databaseBuilder(
            context,
            DemoScenarioDatabase::class.java,
            DEMO_SCENARIO_DATABASE_NAME,
        ).build()

        @Provides
        @Singleton
        fun provideDemoScenarioDao(database: DemoScenarioDatabase): DemoScenarioDao =
            database.demoScenarioDao()

        private const val DEMO_SCENARIO_DATABASE_NAME = "earthquake_demo_scenarios.db"
    }
}
