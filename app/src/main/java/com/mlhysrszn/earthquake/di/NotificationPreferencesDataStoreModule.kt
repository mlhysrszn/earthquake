package com.mlhysrszn.earthquake.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.notificationPreferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "notification_preferences")

@Module
@InstallIn(SingletonComponent::class)
object NotificationPreferencesDataStoreModule {
    @Provides
    @Singleton
    fun provideNotificationPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.notificationPreferencesDataStore
}
