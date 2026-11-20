package com.example.newyorkweather.api

import com.example.newyorkweather.data.GeoResult
import com.example.newyorkweather.data.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("geo/1.0/direct")
    suspend fun getGeoSuggestions(
        @Query("q") q: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<GeoResult>
}
