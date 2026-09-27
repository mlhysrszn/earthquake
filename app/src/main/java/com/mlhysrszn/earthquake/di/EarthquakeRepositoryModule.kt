package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.repository.SampleEarthquakeRepository
import com.mlhysrszn.earthquake.domain.repository.EarthquakeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EarthquakeRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindEarthquakeRepository(
        implementation: SampleEarthquakeRepository,
    ): EarthquakeRepository
}
