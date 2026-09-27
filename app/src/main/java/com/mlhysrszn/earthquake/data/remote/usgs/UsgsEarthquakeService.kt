package com.mlhysrszn.earthquake.data.remote.usgs

import okhttp3.ResponseBody
import retrofit2.http.GET

interface UsgsEarthquakeService {
    @GET("earthquakes/feed/v1.0/summary/all_day.geojson")
    suspend fun fetchAllDaySummary(): ResponseBody
}
