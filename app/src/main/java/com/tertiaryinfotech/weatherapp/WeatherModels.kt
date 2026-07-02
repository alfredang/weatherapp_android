package com.tertiaryinfotech.weatherapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.TimeZone

data class GeoPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?,
) {
    val displaySubtitle: String
        get() = listOfNotNull(admin1, country).joinToString(", ")
}

data class CurrentWeather(
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val isDay: Boolean,
)

data class WeatherUnits(val temperature: String, val windSpeed: String)

data class WeatherResult(
    val place: GeoPlace,
    val current: CurrentWeather,
    val units: WeatherUnits,
    val timezoneId: String?,
) {
    val timeZone: TimeZone?
        get() = timezoneId?.let { id ->
            TimeZone.getTimeZone(id).takeIf { it.id == id || id == "UTC" }
        }
    val condition: WeatherCondition
        get() = WeatherCondition.from(current.weatherCode, current.isDay)
}

/** WMO weather code → human label + Material icon (day/night aware). */
data class WeatherCondition(val label: String, val icon: ImageVector) {
    companion object {
        fun from(code: Int, isDay: Boolean): WeatherCondition = when (code) {
            0 -> WeatherCondition("Clear sky", if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay)
            1 -> WeatherCondition("Mainly clear", if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay)
            2 -> WeatherCondition("Partly cloudy", if (isDay) Icons.Filled.WbCloudy else Icons.Filled.CloudQueue)
            3 -> WeatherCondition("Overcast", Icons.Filled.Cloud)
            45, 48 -> WeatherCondition("Fog", Icons.Filled.Dehaze)
            51, 53, 55 -> WeatherCondition("Drizzle", Icons.Filled.Grain)
            56, 57 -> WeatherCondition("Freezing drizzle", Icons.Filled.AcUnit)
            61, 63, 65 -> WeatherCondition("Rain", Icons.Filled.Umbrella)
            66, 67 -> WeatherCondition("Freezing rain", Icons.Filled.AcUnit)
            71, 73, 75, 77 -> WeatherCondition("Snow", Icons.Filled.AcUnit)
            80, 81, 82 -> WeatherCondition("Rain showers", Icons.Filled.Umbrella)
            85, 86 -> WeatherCondition("Snow showers", Icons.Filled.AcUnit)
            95 -> WeatherCondition("Thunderstorm", Icons.Filled.Thunderstorm)
            96, 99 -> WeatherCondition("Thunderstorm, hail", Icons.Filled.Thunderstorm)
            else -> WeatherCondition("Unknown", Icons.Filled.HelpOutline)
        }
    }
}

/** Weather-concept theme: sky gradient colors matched to the condition (day/night). */
fun skyColors(code: Int?, isDay: Boolean): List<Color> = when (code) {
    0, 1 -> if (isDay) listOf(Color(0xFF2E82F0), Color(0xFF8CCFFF))
            else listOf(Color(0xFF0A0F38), Color(0xFF293570))
    2 -> if (isDay) listOf(Color(0xFF548FDB), Color(0xFFA8C7EB))
         else listOf(Color(0xFF171F47), Color(0xFF3D4A75))
    3, 45, 48 -> if (isDay) listOf(Color(0xFF788AA3), Color(0xFFB3BFD1))
                 else listOf(Color(0xFF262B3B), Color(0xFF474F61))
    in 51..67, in 80..82 -> if (isDay) listOf(Color(0xFF4A6187), Color(0xFF8299B8))
                            else listOf(Color(0xFF141C33), Color(0xFF33405C))
    in 71..77, 85, 86 -> if (isDay) listOf(Color(0xFF99B3D1), Color(0xFFD9E6F5))
                         else listOf(Color(0xFF2E3852), Color(0xFF61708C))
    in 95..99 -> if (isDay) listOf(Color(0xFF3B3D61), Color(0xFF6E6B8F))
                 else listOf(Color(0xFF121029), Color(0xFF362E57))
    else -> if (isDay) listOf(Color(0xFF599EF2), Color(0xFF9ED4FF))
            else listOf(Color(0xFF0F143D), Color(0xFF2E3A6B))
}
