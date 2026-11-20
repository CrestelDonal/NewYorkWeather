package com.example.newyorkweather

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newyorkweather.api.RetrofitInstance
import com.example.newyorkweather.data.WeatherResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class WeatherViewModel : ViewModel() {

    var currentPlaceName: String = "New York"
        private set
    var currentLat: Double? = 40.7143
        private set
    var currentLon: Double? = -74.006
        private set

    val weatherData = MutableLiveData<WeatherResponse?>()
    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()

    var hasLoadedInitialData: Boolean = false

    fun loadWeather(placeName: String, lat: Double?, lon: Double?, isRefresh: Boolean = false) {
        currentPlaceName = placeName
        currentLat = lat
        currentLon = lon

        viewModelScope.launch {
            if (!isRefresh) {
                isLoading.value = true
            }
            try {
                val response = if (lat != null && lon != null) {
                    RetrofitInstance.api.getWeather(
                        lat = lat,
                        lon = lon,
                        apiKey = BuildConfig.OPENWEATHER_API_KEY
                    )
                } else {
                    RetrofitInstance.api.getWeather(
                        city = placeName,
                        apiKey = BuildConfig.OPENWEATHER_API_KEY
                    )
                }
                hasLoadedInitialData = true
                errorMessage.value = null
                weatherData.value = response
            } catch (e: IOException) {
                errorMessage.value = "No internet connection. Please check your network."
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    401 -> "Invalid API key (401)."
                    404 -> "City not found (404)."
                    else -> "HTTP error ${e.code()}"
                }
                errorMessage.value = msg
            } catch (e: Exception) {
                errorMessage.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isLoading.value = false
            }
        }
    }
}
