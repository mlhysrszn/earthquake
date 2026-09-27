package com.mlhysrszn.earthquake.di

import com.mlhysrszn.earthquake.data.remote.usgs.UsgsEarthquakeService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UsgsNetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(USGS_BASE_URL)
        .build()

    @Provides
    @Singleton
    fun provideUsgsEarthquakeService(retrofit: Retrofit): UsgsEarthquakeService =
        retrofit.create(UsgsEarthquakeService::class.java)

    private const val USGS_BASE_URL = "https://earthquake.usgs.gov/"
}
