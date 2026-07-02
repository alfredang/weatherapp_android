package com.tertiaryinfotech.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherAppTheme {
                RootScreen()
            }
        }
    }
}

private data class TabSpec(val label: String, val icon: ImageVector)

@Composable
private fun RootScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        TabSpec("Weather", Icons.Filled.WbCloudy),
        TabSpec("World Time", Icons.Filled.Public),
        TabSpec("Feedback", Icons.AutoMirrored.Filled.Chat),
        TabSpec("About", Icons.Filled.Info),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, spec ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(spec.icon, contentDescription = spec.label) },
                        label = { Text(spec.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                0 -> WeatherScreen()
                1 -> WorldClockScreen()
                2 -> FeedbackScreen()
                else -> AboutScreen()
            }
        }
    }
}
