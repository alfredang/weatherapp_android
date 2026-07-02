package com.tertiaryinfotech.weatherapp

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WorldCity(val name: String, val country: String, val timeZoneId: String)

object WorldCityCatalog {
    val all = listOf(
        WorldCity("Singapore", "Singapore", "Asia/Singapore"),
        WorldCity("Kuala Lumpur", "Malaysia", "Asia/Kuala_Lumpur"),
        WorldCity("Jakarta", "Indonesia", "Asia/Jakarta"),
        WorldCity("Bangkok", "Thailand", "Asia/Bangkok"),
        WorldCity("Hong Kong", "China", "Asia/Hong_Kong"),
        WorldCity("Shanghai", "China", "Asia/Shanghai"),
        WorldCity("Tokyo", "Japan", "Asia/Tokyo"),
        WorldCity("Seoul", "South Korea", "Asia/Seoul"),
        WorldCity("Taipei", "Taiwan", "Asia/Taipei"),
        WorldCity("Manila", "Philippines", "Asia/Manila"),
        WorldCity("Mumbai", "India", "Asia/Kolkata"),
        WorldCity("Dubai", "United Arab Emirates", "Asia/Dubai"),
        WorldCity("Istanbul", "Türkiye", "Europe/Istanbul"),
        WorldCity("Moscow", "Russia", "Europe/Moscow"),
        WorldCity("Berlin", "Germany", "Europe/Berlin"),
        WorldCity("Paris", "France", "Europe/Paris"),
        WorldCity("London", "United Kingdom", "Europe/London"),
        WorldCity("New York", "United States", "America/New_York"),
        WorldCity("Toronto", "Canada", "America/Toronto"),
        WorldCity("Chicago", "United States", "America/Chicago"),
        WorldCity("Denver", "United States", "America/Denver"),
        WorldCity("Los Angeles", "United States", "America/Los_Angeles"),
        WorldCity("Vancouver", "Canada", "America/Vancouver"),
        WorldCity("Mexico City", "Mexico", "America/Mexico_City"),
        WorldCity("São Paulo", "Brazil", "America/Sao_Paulo"),
        WorldCity("Cairo", "Egypt", "Africa/Cairo"),
        WorldCity("Nairobi", "Kenya", "Africa/Nairobi"),
        WorldCity("Johannesburg", "South Africa", "Africa/Johannesburg"),
        WorldCity("Sydney", "Australia", "Australia/Sydney"),
        WorldCity("Melbourne", "Australia", "Australia/Melbourne"),
        WorldCity("Auckland", "New Zealand", "Pacific/Auckland"),
        WorldCity("Honolulu", "United States", "Pacific/Honolulu"),
    )
    val defaultIds = listOf(
        "Asia/Singapore", "Europe/London", "America/New_York", "Asia/Tokyo", "Australia/Sydney",
    )

    fun byId(id: String): WorldCity? = all.firstOrNull { it.timeZoneId == id }
}

private const val PREFS = "world_clock"
private const val KEY_IDS = "city_ids"

private fun loadCityIds(context: Context): List<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_IDS, null)?.split(",")?.filter { it.isNotBlank() }
        ?: WorldCityCatalog.defaultIds

private fun saveCityIds(context: Context, ids: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_IDS, ids.joinToString(",")).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen() {
    val context = LocalContext.current
    var cityIds by remember { mutableStateOf(loadCityIds(context)) }
    var showPicker by remember { mutableStateOf(false) }
    val cities = cityIds.mapNotNull(WorldCityCatalog::byId)

    val nowMillis by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Time") },
                actions = {
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add a city")
                    }
                },
            )
        },
    ) { padding ->
        if (cities.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Public, contentDescription = null, Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("No cities yet — tap + to add a city and see its local time.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(cities, key = { it.timeZoneId }) { city ->
                    WorldClockRow(city, nowMillis) {
                        cityIds = cityIds - city.timeZoneId
                        saveCityIds(context, cityIds)
                    }
                }
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            CityPicker(
                existing = cityIds,
                onPick = { city ->
                    cityIds = cityIds + city.timeZoneId
                    saveCityIds(context, cityIds)
                    showPicker = false
                },
            )
        }
    }
}

@Composable
private fun WorldClockRow(city: WorldCity, nowMillis: Long, onDelete: () -> Unit) {
    val tz = TimeZone.getTimeZone(city.timeZoneId)
    val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val isDaytime = hour in 6..17

    Card {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                contentDescription = if (isDaytime) "Day" else "Night",
                tint = if (isDaytime) androidx.compose.ui.graphics.Color(0xFFF9A825)
                       else androidx.compose.ui.graphics.Color(0xFF5C6BC0),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(city.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${city.country} · ${offsetText(tz, nowMillis)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(timeText(nowMillis, tz), fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                Text(dayText(nowMillis, tz), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove ${city.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun timeText(nowMillis: Long, tz: TimeZone): String =
    SimpleDateFormat("h:mm:ss a", Locale.ENGLISH)
        .apply { timeZone = tz }.format(Date(nowMillis))

private fun dayText(nowMillis: Long, tz: TimeZone): String =
    SimpleDateFormat("EEE, d MMM", Locale.ENGLISH)
        .apply { timeZone = tz }.format(Date(nowMillis))

private fun offsetText(tz: TimeZone, nowMillis: Long): String {
    val seconds = (tz.getOffset(nowMillis) - TimeZone.getDefault().getOffset(nowMillis)) / 1000
    if (seconds == 0) return "Same time"
    val hours = seconds / 3600.0
    val text = if (hours == hours.toLong().toDouble()) hours.toLong().toString()
               else String.format(Locale.ENGLISH, "%.1f", hours)
    return (if (seconds > 0) "+" else "") + text + "h"
}

@Composable
private fun CityPicker(existing: List<String>, onPick: (WorldCity) -> Unit) {
    var search by remember { mutableStateOf("") }
    val available = WorldCityCatalog.all
        .filter { it.timeZoneId !in existing }
        .filter {
            search.isBlank() || it.name.contains(search, ignoreCase = true) ||
                it.country.contains(search, ignoreCase = true)
        }

    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Text("Add City", fontWeight = FontWeight.Bold, fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text("Search cities") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(available, key = { it.timeZoneId }) { city ->
                Column(
                    Modifier.fillMaxWidth().clickable { onPick(city) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(city.name)
                    Text(city.country, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }
    }
}
