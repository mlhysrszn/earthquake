package com.mlhysrszn.earthquake.data.remote.usgs

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface UsgsEarthquakeService {
    @GET("earthquakes/feed/v1.0/summary/all_day.geojson")
    suspend fun fetchAllDaySummary(): ResponseBody

    @GET("fdsnws/event/1/query?format=geojson")
    suspend fun fetchEventById(@Query("eventid") eventId: String): ResponseBody
}
