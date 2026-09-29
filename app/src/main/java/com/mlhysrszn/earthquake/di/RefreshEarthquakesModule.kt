package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.notification.NotificationAwareEarthquakeRefresher
import com.mlhysrszn.earthquake.domain.usecase.RefreshEarthquakes
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RefreshEarthquakesModule {
    @Binds
    @Singleton
    abstract fun bindRefreshEarthquakes(
        implementation: NotificationAwareEarthquakeRefresher,
    ): RefreshEarthquakes
}
