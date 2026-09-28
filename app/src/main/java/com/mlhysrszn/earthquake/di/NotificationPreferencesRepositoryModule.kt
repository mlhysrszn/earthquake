package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.local.preferences.DataStoreNotificationPreferencesRepository
import com.mlhysrszn.earthquake.domain.repository.NotificationPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationPreferencesRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNotificationPreferencesRepository(
        implementation: DataStoreNotificationPreferencesRepository,
    ): NotificationPreferencesRepository
}
