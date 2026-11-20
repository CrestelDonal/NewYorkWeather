package com.example.newyorkweather

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.newyorkweather.api.RetrofitInstance
import com.example.newyorkweather.data.WeatherResponse
import com.example.newyorkweather.data.toFormattedString
import com.example.newyorkweather.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: WeatherViewModel
    private lateinit var adapter: GeoSuggestionAdapter

    private var searchJob: Job? = null
    private var isSelectingSuggestion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        adapter = GeoSuggestionAdapter(this, android.R.layout.simple_dropdown_item_1line)
        binding.searchAutoCompleteTextView.setAdapter(adapter)

        setupSearchListeners()
        setupObservers()

        if (!viewModel.hasLoadedInitialData) {
            viewModel.loadWeather("New York", 40.7143, -74.006)
        }
    }

    private fun setupSearchListeners() {
        binding.searchAutoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isSelectingSuggestion) return

                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()

                if (query.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(400)
                        fetchSuggestions(query)
                    }
                } else {
                    adapter.updateSuggestions(emptyList(), emptyList())
                }
            }
        })

        binding.searchAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedText = adapter.getItem(position)
            if (selectedText == "No places found") {
                return@setOnItemClickListener
            }
            val geoResult = adapter.getGeoResult(position)
            if (geoResult != null) {
                val placeName = geoResult.toFormattedString()
                isSelectingSuggestion = true
                binding.searchAutoCompleteTextView.setText(placeName, false)
                isSelectingSuggestion = false
                hideKeyboard()
                viewModel.loadWeather(placeName, geoResult.lat, geoResult.lon)
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadWeather(
                viewModel.currentPlaceName,
                viewModel.currentLat,
                viewModel.currentLon,
                isRefresh = true
            )
        }
    }

    private suspend fun fetchSuggestions(query: String) {
        try {
            val results = RetrofitInstance.api.getGeoSuggestions(
                q = query,
                limit = 5,
                apiKey = BuildConfig.OPENWEATHER_API_KEY
            )
            if (results.isEmpty()) {
                adapter.updateSuggestions(listOf("No places found"), listOf(null))
            } else {
                val displayStrings = results.map { it.toFormattedString() }
                adapter.updateSuggestions(displayStrings, results)
            }
            binding.searchAutoCompleteTextView.showDropDown()
        } catch (e: IOException) {
            Log.e("GeoSearch", "Network error while getting suggestions", e)
            Toast.makeText(this@MainActivity, "No internet connection.", Toast.LENGTH_SHORT).show()
        } catch (e: HttpException) {
            val msg = when (e.code()) {
                401 -> "Invalid API key (401)."
                else -> "HTTP error ${e.code()}"
            }
            Log.e("GeoSearch", msg, e)
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("GeoSearch", "Error fetching suggestions", e)
        }
    }

    private fun setupObservers() {
        viewModel.weatherData.observe(this) { response ->
            if (response != null) {
                displayWeather(response, viewModel.currentPlaceName)
            }
        }
        viewModel.errorMessage.observe(this) { msg ->
            if (msg != null) {
                showError(msg)
            }
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility =
                if (loading == true && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
            if (loading == false) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun displayWeather(response: WeatherResponse, placeName: String) {
        val weatherItem = response.weather?.firstOrNull() ?: return

        binding.cityText.text = placeName
        binding.mainText.text = weatherItem.main ?: ""

        val description = weatherItem.description
        binding.descriptionText.text = description?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        } ?: ""

        val temp = response.main?.temp
        val feelsLike = response.main?.feelsLike
        val humidity = response.main?.humidity
        val speed = response.wind?.speed

        binding.tempText.text = temp?.let { "${it.toInt()}°C" } ?: "--"
        binding.feelsLikeText.text = feelsLike?.let { "Feels like ${it.toInt()}°C" } ?: "Feels like --"
        binding.humidityText.text = humidity?.let { "Humidity: $it%" } ?: "Humidity: --"
        binding.windText.text = speed?.let { "Wind: $it m/s" } ?: "Wind: --"

        updateThemeBackground(weatherItem.main)

        val rawIcon = weatherItem.icon?.trim()
        val iconCode = if (rawIcon == "01n") "01d" else rawIcon
        if (!iconCode.isNullOrEmpty()) {
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@4x.png"
            Glide.with(this@MainActivity)
                .load(iconUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("WeatherIcon", "Failed to load weather icon from $iconUrl", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .error(R.drawable.ic_weather_error)
                .into(binding.weatherIcon)
        } else {
            binding.weatherIcon.setImageResource(R.drawable.ic_weather_error)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun updateThemeBackground(mainCondition: String?) {
        val colorHex = when (mainCondition) {
            "Clear" -> "#E3F2FD"
            "Clouds" -> "#ECEFF1"
            "Rain", "Drizzle" -> "#CFD8DC"
            "Thunderstorm" -> "#90A4AE"
            "Snow" -> "#FAFAFA"
            else -> "#F5F5F5"
        }
        binding.contentContainer.setBackgroundColor(Color.parseColor(colorHex))
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.cityText.text = getString(R.string.error_title)
        binding.mainText.text = message
        binding.descriptionText.text = ""
        binding.tempText.text = "--"
        binding.feelsLikeText.text = "Feels like --"
        binding.humidityText.text = "Humidity: --"
        binding.windText.text = "Wind: --"
        binding.weatherIcon.setImageResource(R.drawable.ic_weather_error)
        updateThemeBackground(null)
    }
}
