package com.mlhysrszn.earthquake.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationPreferencesDataStoreModule {
    @Provides
    @Singleton
    fun provideNotificationPreferencesDataStore(
        @ApplicationContext context: Context,
        variantConfiguration: AppVariantConfiguration,
    ): DataStore<Preferences> = dataStores.computeIfAbsent(variantConfiguration.preferencesFileName) { name ->
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile(name) })
    }

    // One DataStore per file for the whole process, even if the Hilt component is recreated in tests.
    private val dataStores = ConcurrentHashMap<String, DataStore<Preferences>>()
}
