package com.example.newyorkweather.data

import com.squareup.moshi.Json

data class WeatherResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "weather") val weather: List<Weather>? = null,
    @Json(name = "main") val main: MainData? = null,
    @Json(name = "wind") val wind: WindData? = null
)

data class Weather(
    @Json(name = "main") val main: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "icon") val icon: String? = null
)

data class MainData(
    @Json(name = "temp") val temp: Double? = null,
    @Json(name = "feels_like") val feelsLike: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null
)

data class WindData(
    @Json(name = "speed") val speed: Double? = null
)
