package com.mlhysrszn.earthquake.di

import android.content.Context
import androidx.room.Room
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDao
import com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideEarthquakeDatabase(
        @ApplicationContext context: Context,
    ): EarthquakeDatabase = Room.databaseBuilder(
        context,
        EarthquakeDatabase::class.java,
        DATABASE_NAME,
    ).build()

    @Provides
    @Singleton
    fun provideEarthquakeDao(database: EarthquakeDatabase): EarthquakeDao =
        database.earthquakeDao()

    private const val DATABASE_NAME = "earthquake.db"
}
