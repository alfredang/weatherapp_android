# Tertiary Weather (Android)

A native **Android** weather app built with **Jetpack Compose** and **Material 3** — the
Android twin of the iOS [Tertiary Weather](https://apps.apple.com/us/app/tertiary-weather/id6785375222)
app. Search any city (or use your current location), pull live conditions from the free
**[Open-Meteo](https://open-meteo.com)** API, and see the place pinned on an OpenStreetMap
view — behind a clean Material bottom navigation.

## Features

- 📍 **Auto-detect current location** — on launch the app finds where you are (LocationManager +
  reverse geocoding) and shows the local weather and time; a location button re-detects on demand.
- 🔎 **Search any city or place** — free-text geocoding via Open-Meteo.
- 🌡️ **Live current weather** — temperature, feels-like, humidity, wind, and a WMO condition
  label with a matching Material icon (day/night aware).
- 🕐 **Live local time** — a running clock for the loaded place, in its own time zone.
- 🌍 **World Time** — a world-clock tab with live times for your chosen cities
  (add/remove, day/night icons, UTC-offset vs your device), persisted across launches.
- 🗺️ **Location on a map** — the resolved place is dropped as a marker on an osmdroid
  (OpenStreetMap) map.
- 🧭 **Material bottom navigation** — Weather, World Time, Feedback (→ WhatsApp), and About.
- 🌦️ **Weather-concept theme** — a dynamic sky-gradient background that matches the current
  condition (clear/cloudy/rain/snow/storm, day and night variants).

## Tech Stack

| Layer | Choice |
|-------|--------|
| UI | Jetpack Compose (Material 3), min SDK 26 / target SDK 36 |
| Map | [osmdroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap, no API key) |
| Location | `LocationManagerCompat` + platform `Geocoder` |
| Networking | `HttpURLConnection` + `org.json` (no extra deps) |
| Data source | [Open-Meteo](https://open-meteo.com) geocoding + forecast APIs (no key required) |

## Getting Started

```bash
# Debug build
./gradlew :app:assembleDebug

# Release AAB (needs keystore.properties + the upload keystore, not committed)
./gradlew :app:bundleRelease
```

Open in Android Studio and run on any device/emulator with API 26+.

## Data Source

Weather and geocoding data © [Open-Meteo.com](https://open-meteo.com), licensed under CC BY 4.0.
Map tiles © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors.

## Acknowledgements

Developed by [Tertiary Infotech Academy Pte Ltd](https://www.tertiaryinfotech.com).
