# RowingSync - ESP32 Rowing Machine Companion App

This Android app syncs rowing workout sessions from the [ESP32RowingMachine](https://github.com/j0b333/ESP32RowingMachine) to Google Health Connect (which syncs to Samsung Health, Google Fit, and other fitness apps).

## Features

- **Fetch Workouts**: Connect to the ESP32 rowing monitor via WiFi and fetch stored workout sessions
- **View Workout List**: Display all workout sessions with key metrics (distance, duration, strokes, calories)
- **Sync to Health Connect**: Push workout data to Google Health Connect with a single tap
- **Per-Second Data Support**: Stores detailed heart rate samples per-second (not just summary data)

## Health Connect Data Storage

Health Connect supports storing both **summary data** and **per-second sample data** for rowing workouts:

| Data Type | Storage Type | Description |
|-----------|--------------|-------------|
| Exercise Session | Summary | Rowing machine workout with start/end time |
| Distance | Summary | Total distance covered (meters) |
| Calories Burned | Summary | Total calories burned |
| Heart Rate | Per-Second Samples | Individual BPM readings with timestamps |

**Note**: While Health Connect supports per-second samples for heart rate, stroke rate, and power, the ESP32 currently stores heart rate samples which are synced with their original timestamps.

## Project Structure

```
RowingSync/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── health_permissions.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       └── network_security_config.xml
│   │   └── java/com/example/rowingsync/
│   │       ├── MainActivity.kt
│   │       ├── RowingSyncApplication.kt
│   │       ├── data/
│   │       │   ├── Models.kt
│   │       │   ├── Esp32Api.kt
│   │       │   └── ApiClient.kt
│   │       ├── health/
│   │       │   └── HealthConnectManager.kt
│   │       └── ui/
│   │           ├── MainViewModel.kt
│   │           └── screens/
│   │               └── SessionListScreen.kt
│   └── proguard-rules.pro
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties
```

## Quick Start

1. Install Android Studio (latest stable version)
2. Open this directory as a project
3. Let Gradle sync dependencies
4. Connect an Android device or start an emulator
5. Run the app

## Requirements

- Android 9+ (API 28+)
- Health Connect app installed (comes with Android 14+, can be installed from Play Store on older versions)
- Same WiFi network as ESP32 (or connect to ESP32's access point)

## Usage

1. **Connect to ESP32**: Either join the ESP32's WiFi access point (`CrivitRower` / `rowing123`) or ensure both devices are on the same network
2. **Enter ESP32 Address**: Default is `192.168.4.1` for AP mode, or use `rowing.local` if mDNS is configured
3. **Tap Connect**: The app will fetch all stored workout sessions from the ESP32
4. **View Workouts**: Browse through your rowing sessions with distance, duration, strokes, and calories
5. **Sync to Health Connect**: Tap "Sync to Health Connect" button on any unsynced workout to push data

## Configuration

The app allows entering the ESP32 address directly in the UI. Default addresses:
- `192.168.4.1` - ESP32 access point mode (default)
- `rowing.local` - Station mode with mDNS enabled

## Health Connect Permissions

The app requests these permissions on first launch:
- Read/Write Exercise Sessions
- Read/Write Heart Rate (for per-second samples)
- Read/Write Distance
- Read/Write Calories Burned

## ESP32 API Endpoints

The app communicates with the ESP32 via HTTP REST API:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/status` | GET | Check device status and workout state |
| `/api/sessions` | GET | List all stored workout sessions |
| `/api/sessions/{id}` | GET | Get detailed session with heart rate samples |
| `/api/sessions/{id}/synced` | POST | Mark session as synced |

## License

MIT License - See [LICENSE](LICENSE) for details.
