package com.tertiaryinfotech.weatherapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkyBlue = Color(0xFF2E7CF0)

private val LightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E7FD),
    onPrimaryContainer = Color(0xFF0B2A5C),
    surface = Color(0xFFF7F9FC),
    background = Color(0xFFF2F5FA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB8F8),
    onPrimary = Color(0xFF0B2A5C),
    primaryContainer = Color(0xFF1E4380),
    onPrimaryContainer = Color(0xFFD8E7FD),
    surface = Color(0xFF14161B),
    background = Color(0xFF0F1115),
)

@Composable
fun WeatherAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
