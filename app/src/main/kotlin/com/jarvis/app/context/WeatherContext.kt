package com.jarvis.app.context

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    val weather: List<WeatherDesc>,
    val main: MainTemp,
    val name: String
)

data class WeatherDesc(val description: String)
data class MainTemp(val temp: Double, val humidity: Int)

interface OpenWeatherApi {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

class WeatherContext(private val apiKey: String) {

    private val api = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(OpenWeatherApi::class.java)

    suspend fun getCurrentWeather(city: String = "New York"): String {
        if (apiKey.isBlank()) return "Weather context unavailable. Key not configured."
        
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getWeather(city, apiKey)
                "Current weather in ${response.name}: ${response.weather.firstOrNull()?.description}, ${response.main.temp}°C, Humidity: ${response.main.humidity}%."
            } catch (e: Exception) {
                Log.e("WeatherContext", "Failed to fetch weather", e)
                "Weather data unavailable."
            }
        }
    }
}
