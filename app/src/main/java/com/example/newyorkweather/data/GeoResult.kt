package com.example.newyorkweather.data

import com.squareup.moshi.Json

data class GeoResult(
    @Json(name = "name") val name: String,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "country") val country: String? = null,
    @Json(name = "state") val state: String? = null
)

fun GeoResult.toFormattedString(): String {
    val parts = mutableListOf<String>()
    parts.add(name)
    if (!state.isNullOrBlank()) {
        parts.add(state)
    }
    if (!country.isNullOrBlank()) {
        parts.add(country)
    }
    return parts.joinToString(", ")
}
