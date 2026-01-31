# RowingSync Companion App

This document provides detailed information about the RowingSync Android companion app.

## Features

### Connect to ESP32
- Enter the ESP32 rowing monitor's IP address or hostname
- Supports both Access Point mode (`192.168.4.1`) and Station mode (`rowing.local`)
- Real-time connection status display

### View Workouts
- Display all stored workout sessions from the ESP32
- Show key metrics for each workout:
  - Distance (meters)
  - Duration (HH:MM:SS)
  - Stroke count
  - Calories burned
  - Average power (watts)
  - Average pace (per 500m)
  - Heart rate (average and max)

### Sync to Health Connect
- One-tap sync for individual workouts
- Syncs complete session data including:
  - Exercise session with proper rowing type
  - Total distance
  - Total calories
  - Heart rate samples (per-second)
  - Power samples
  - Speed samples
- Visual indication of synced vs unsynced workouts

## Installation

### From Source
1. Clone the repository
2. Open in Android Studio (latest stable version)
3. Let Gradle sync dependencies
4. Connect Android device or start emulator
5. Run the app

### Requirements
- Android 9+ (API 28+)
- Health Connect app installed
- Same WiFi network as ESP32 (or ESP32's access point)

## Usage Guide

### First Launch

1. **Grant Permissions**: The app will request Health Connect permissions
2. **Install Health Connect**: If prompted, install from Play Store (Android 9-13)

### Connecting to ESP32

#### Access Point Mode (Recommended for first use)
1. On your phone, go to WiFi settings
2. Connect to the ESP32's network:
   - SSID: `CrivitRower`
   - Password: `rowing123`
3. Open RowingSync app
4. Enter address: `192.168.4.1`
5. Tap "Connect"

#### Station Mode (For regular use)
1. Configure ESP32 to connect to your home WiFi
2. Both phone and ESP32 should be on same network
3. Open RowingSync app
4. Enter address: `rowing.local` (or ESP32's IP)
5. Tap "Connect"

### Viewing Workouts

After connecting, all stored workouts appear in a list showing:
- Date and time
- Total distance
- Duration
- Strokes
- Calories

Tap a workout to see more details.

### Syncing to Health Connect

1. Find an unsynced workout (no checkmark icon)
2. Tap "Sync to Health Connect" button
3. Wait for sync to complete
4. Checkmark appears when successful

Synced workouts will appear in:
- Google Health Connect
- Samsung Health
- Google Fit
- Other connected fitness apps

## App Architecture

The app uses modern Android development practices:

- **Jetpack Compose**: Declarative UI framework
- **Material 3**: Modern design system
- **MVVM Pattern**: Clean separation of concerns
- **Kotlin Coroutines**: Async operations
- **Retrofit**: REST API communication
- **Health Connect SDK**: Health data integration

## Project Structure

```
app/src/main/java/com/example/rowingsync/
├── MainActivity.kt           # App entry point
├── RowingSyncApplication.kt  # App initialization
├── data/
│   ├── Models.kt             # Data classes
│   ├── Esp32Api.kt           # API interface
│   └── ApiClient.kt          # HTTP client
├── health/
│   └── HealthConnectManager.kt  # Health Connect integration
└── ui/
    ├── MainViewModel.kt      # State management
    └── screens/
        └── SessionListScreen.kt  # Main UI
```

## Configuration

### Network Settings
- Default timeout: 5s (connect), 30s (read), 10s (write)
- HTTP logging enabled in debug builds

### Health Connect
- Uses Health Connect SDK 1.1.0-alpha
- Requires all exercise-related permissions

## Troubleshooting

### Cannot Connect to ESP32
- Verify you're on the correct WiFi network
- Check the IP address is correct
- Ensure ESP32 is powered on
- Try pinging the ESP32 from another device

### Health Connect Errors
- Ensure Health Connect app is installed
- Check app permissions in system settings
- Verify Health Connect is not in low storage mode

### Sync Fails
- Check internet connection (if syncing to cloud services)
- Verify workout has all required data
- Try restarting the app
- Check Health Connect app for errors

## Privacy

- Workout data is only stored locally on ESP32 and synced to Health Connect
- No data is sent to external servers
- Health Connect handles data sharing with other apps based on user permissions
