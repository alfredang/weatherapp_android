package com.tertiaryinfotech.weatherapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WeatherException(message: String) : Exception(message)

/** Talks to the free Open-Meteo geocoding + forecast APIs (no key required). */
object WeatherService {
    private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"

    private suspend fun fetchJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            if (conn.responseCode !in 200..299) {
                throw WeatherException("The weather service returned an unexpected response.")
            }
            JSONObject(conn.inputStream.bufferedReader().readText())
        } finally {
            conn.disconnect()
        }
    }

    suspend fun geocode(query: String): GeoPlace {
        val q = URLEncoder.encode(query, "UTF-8")
        val json = fetchJson("$GEOCODING_URL?name=$q&count=1&language=en&format=json")
        val results = json.optJSONArray("results")
            ?: throw WeatherException("We couldn't find that place. Try another city or town.")
        if (results.length() == 0) {
            throw WeatherException("We couldn't find that place. Try another city or town.")
        }
        val r = results.getJSONObject(0)
        return GeoPlace(
            name = r.getString("name"),
            latitude = r.getDouble("latitude"),
            longitude = r.getDouble("longitude"),
            country = r.optString("country").ifEmpty { null },
            admin1 = r.optString("admin1").ifEmpty { null },
        )
    }

    suspend fun currentWeather(place: GeoPlace): WeatherResult {
        val json = fetchJson(
            "$FORECAST_URL?latitude=${place.latitude}&longitude=${place.longitude}" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
                "weather_code,wind_speed_10m&timezone=auto"
        )
        val cur = json.getJSONObject("current")
        val units = json.getJSONObject("current_units")
        return WeatherResult(
            place = place,
            current = CurrentWeather(
                temperature = cur.getDouble("temperature_2m"),
                apparentTemperature = cur.getDouble("apparent_temperature"),
                humidity = cur.getInt("relative_humidity_2m"),
                windSpeed = cur.getDouble("wind_speed_10m"),
                weatherCode = cur.getInt("weather_code"),
                isDay = cur.getInt("is_day") == 1,
            ),
            units = WeatherUnits(
                temperature = units.optString("temperature_2m", "°C"),
                windSpeed = units.optString("wind_speed_10m", "km/h"),
            ),
            timezoneId = json.optString("timezone").ifEmpty { null },
        )
    }

    suspend fun weatherFor(query: String): WeatherResult = currentWeather(geocode(query))
}
