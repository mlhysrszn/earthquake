package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.domain.model.ProductEventEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppVariantModule {
    @Provides
    @Singleton
    fun provideAppVariantConfiguration() = AppVariantConfiguration(
        environment = ProductEventEnvironment.DEMO,
        databaseName = "earthquake_demo.db",
        preferencesFileName = "notification_preferences_demo",
        periodicWorkName = "earthquake-periodic-sync-demo",
    )
}
