package com.tertiaryinfotech.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Loaded(val result: WeatherResult) : WeatherUiState
    data class Failed(val message: String) : WeatherUiState
}

class WeatherViewModel : ViewModel() {
    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val state: StateFlow<WeatherUiState> = _state

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun onQueryChange(value: String) { _query.value = value }

    var didAutoLocate = false
        private set

    fun search() {
        val trimmed = _query.value.trim()
        if (trimmed.isEmpty()) return
        _state.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                _state.value = WeatherUiState.Loaded(WeatherService.weatherFor(trimmed))
            } catch (e: Exception) {
                _state.value = WeatherUiState.Failed(e.message ?: "Something went wrong.")
            }
        }
    }

    /**
     * Load weather for the device location. When [quietly] is true (the launch-time
     * auto attempt) failures fall back to the idle hint instead of an error card.
     */
    fun locateMe(context: Context, quietly: Boolean = false) {
        didAutoLocate = true
        _state.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                val location = currentLocation(context)
                    ?: throw WeatherException("We couldn't determine your location. Try again, or search for a place instead.")
                val place = reverseGeocode(context, location)
                _query.value = place.name
                _state.value = WeatherUiState.Loaded(WeatherService.currentWeather(place))
            } catch (e: Exception) {
                if (quietly) {
                    _state.value = WeatherUiState.Idle
                } else {
                    _state.value = WeatherUiState.Failed(e.message ?: "Something went wrong.")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")   // callers gate on the runtime permission
    private suspend fun currentLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= 31 &&
                lm.allProviders.contains(LocationManager.FUSED_PROVIDER)) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
        }
        for (provider in providers) {
            val location = withTimeoutOrNull(8_000) { requestLocation(context, lm, provider) }
            if (location != null) return location
        }
        // A provider may have nothing fresh — fall back to the last known fix.
        return providers.firstNotNullOfOrNull { lm.getLastKnownLocation(it) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLocation(
        context: Context, lm: LocationManager, provider: String,
    ): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        LocationManagerCompat.getCurrentLocation(
            lm, provider, signal,
            androidx.core.content.ContextCompat.getMainExecutor(context)
        ) { location -> cont.resume(location) }
    }

    private suspend fun reverseGeocode(context: Context, location: Location): GeoPlace =
        withContext(Dispatchers.IO) {
            val address = try {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.ENGLISH)
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
            } catch (_: Exception) {
                null
            }
            val name = address?.locality ?: address?.subAdminArea ?: "My Location"
            // Drop 2–3 letter region codes and admin areas that just repeat the city.
            val admin = address?.adminArea?.takeIf { it.length > 3 && it != name }
            GeoPlace(
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
                country = address?.countryName,
                admin1 = admin,
            )
        }
}
