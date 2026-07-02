package com.tertiaryinfotech.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun WeatherScreen(vm: WeatherViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            vm.locateMe(context, quietly = vm.didAutoLocate)
        }
    }

    // Launch-time behaviour: try the current location once, silently.
    LaunchedEffect(Unit) {
        if (!vm.didAutoLocate && state is WeatherUiState.Idle) {
            if (hasLocationPermission(context)) {
                vm.locateMe(context, quietly = true)
            } else {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ))
            }
        }
    }

    val result = (state as? WeatherUiState.Loaded)?.result
    val sky = Brush.verticalGradient(
        skyColors(result?.current?.weatherCode, result?.current?.isDay ?: true)
    )

    Box(Modifier.fillMaxSize().background(sky)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Weather",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )

            SearchField(
                query = query,
                onQueryChange = vm::onQueryChange,
                onSearch = vm::search,
                onLocate = {
                    if (hasLocationPermission(context)) {
                        vm.locateMe(context)
                    } else {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ))
                    }
                },
            )

            when (val s = state) {
                is WeatherUiState.Idle -> IdleHint()
                is WeatherUiState.Loading -> Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Color.White) }
                is WeatherUiState.Failed -> ErrorCard(s.message)
                is WeatherUiState.Loaded -> {
                    WeatherCard(s.result)
                    LocationMap(s.result)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLocate: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Enter a city or place") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            Row {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
                IconButton(onClick = onLocate) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Use my current location")
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.92f),
            focusedContainerColor = Color.White,
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearch() }
        ),
    )
}

@Composable
private fun IdleHint() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.WaterDrop, contentDescription = null,
            tint = Color.White, modifier = Modifier.size(52.dp),
        )
        Text(
            "Search for a place — or tap the location icon — to see its weather, " +
                "local time, and map.",
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFFE65100))
            Spacer(Modifier.width(12.dp))
            Text(message, color = Color.Black)
        }
    }
}

/** Ticks once a second while visible; returns the current epoch millis. */
@Composable
private fun rememberNowMillis(): Long {
    val now = produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    return now.value
}

@Composable
private fun WeatherCard(result: WeatherResult) {
    val nowMillis = rememberNowMillis()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.90f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(result.place.name, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = Color.Black)
                    val sub = result.place.displaySubtitle
                    if (sub.isNotEmpty()) {
                        Text(sub, color = Color.DarkGray, fontSize = 14.sp)
                    }
                }
                Icon(
                    result.condition.icon, contentDescription = result.condition.label,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp),
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${result.current.temperature.roundToInt()}${result.units.temperature}",
                    fontSize = 58.sp, fontWeight = FontWeight.Light, color = Color.Black,
                )
                Spacer(Modifier.width(8.dp))
                Text(result.condition.label, fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray, modifier = Modifier.padding(bottom = 10.dp))
            }

            result.timeZone?.let { tz ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = "Local time",
                        tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(localTimeText(nowMillis, tz), color = Color.DarkGray, fontSize = 14.sp)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric(Icons.Filled.Thermostat, "Feels like",
                    "${result.current.apparentTemperature.roundToInt()}${result.units.temperature}")
                Metric(Icons.Filled.WaterDrop, "Humidity", "${result.current.humidity}%")
                Metric(Icons.Filled.Air, "Wind",
                    "${result.current.windSpeed.roundToInt()} ${result.units.windSpeed}")
            }
        }
    }
}

private fun localTimeText(nowMillis: Long, tz: TimeZone): String {
    val fmt = SimpleDateFormat("EEE, d MMM 'at' h:mm:ss a", Locale.ENGLISH)
    fmt.timeZone = tz
    return fmt.format(Date(nowMillis))
}

@Composable
private fun Metric(icon: androidx.compose.ui.graphics.vector.ImageVector,
                   label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 14.sp)
        Text(label, color = Color.DarkGray, fontSize = 12.sp)
    }
}

@Composable
private fun LocationMap(result: WeatherResult) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(11.0)
                }
            },
            update = { map ->
                val point = GeoPoint(result.place.latitude, result.place.longitude)
                map.controller.setCenter(point)
                map.overlays.removeAll { it is Marker }
                map.overlays.add(Marker(map).apply {
                    position = point
                    title = result.place.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
                map.invalidate()
            },
        )
    }
}
