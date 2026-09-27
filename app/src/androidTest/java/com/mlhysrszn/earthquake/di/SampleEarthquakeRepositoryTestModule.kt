package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.repository.SampleEarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [EarthquakeRepositoryModule::class],
)
abstract class SampleEarthquakeRepositoryTestModule {
    @Binds
    @Singleton
    abstract fun bindEarthquakeRepository(
        implementation: SampleEarthquakeRepository,
    ): EarthquakeRepository
}
