# System Architecture

This document describes the architecture of the RowingSync companion app and its integration with the ESP32 rowing monitor.

## Overview

The system consists of two main components:
1. **ESP32 Rowing Monitor** - Hardware device that measures rowing metrics
2. **RowingSync Android App** - Companion app that syncs data to Health Connect

```
┌─────────────────────┐      WiFi/HTTP       ┌─────────────────────┐
│   ESP32 Rowing      │  ─────────────────>  │   RowingSync        │
│   Monitor           │  <─────────────────  │   Android App       │
│   (Hardware)        │      REST API        │   (Software)        │
└─────────────────────┘                      └─────────────────────┘
                                                      │
                                                      │ Health Connect API
                                                      ▼
                                             ┌─────────────────────┐
                                             │   Google Health     │
                                             │   Connect           │
                                             └─────────────────────┘
                                                      │
                                                      │ Syncs to
                                                      ▼
                                             ┌─────────────────────┐
                                             │ • Samsung Health    │
                                             │ • Google Fit        │
                                             │ • Other fitness apps│
                                             └─────────────────────┘
```

## App Architecture

The Android app follows the **MVVM (Model-View-ViewModel)** architecture pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              SessionListScreen.kt                      │  │
│  │  • ConnectionCard (ESP32 connection UI)                │  │
│  │  • SessionCard (workout display & sync)                │  │
│  │  • Error/status displays                               │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ observes StateFlow
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     ViewModel Layer                         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              MainViewModel.kt                          │  │
│  │  • Connection state management                         │  │
│  │  • Session loading & caching                           │  │
│  │  • Health Connect sync coordination                    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│      Data Layer         │     │    Health Layer         │
│  ┌───────────────────┐  │     │  ┌───────────────────┐  │
│  │  Esp32Api.kt      │  │     │  │HealthConnectMgr   │  │
│  │  ApiClient.kt     │  │     │  │  • Permissions    │  │
│  │  Models.kt        │  │     │  │  • Record sync    │  │
│  └───────────────────┘  │     │  └───────────────────┘  │
└─────────────────────────┘     └─────────────────────────┘
              │                               │
              ▼                               ▼
       ESP32 (WiFi/HTTP)              Health Connect API
```

## Key Components

### 1. MainActivity.kt
Entry point for the application. Sets up the Compose theme and displays the main screen.

### 2. Data Layer

| File | Purpose |
|------|---------|
| `Models.kt` | Data classes for API responses and UI state |
| `Esp32Api.kt` | Retrofit interface defining REST endpoints |
| `ApiClient.kt` | Singleton HTTP client with URL management |

### 3. ViewModel Layer

| File | Purpose |
|------|---------|
| `MainViewModel.kt` | State management, API calls, sync coordination |

### 4. UI Layer

| File | Purpose |
|------|---------|
| `SessionListScreen.kt` | Main UI with connection card and session list |

### 5. Health Layer

| File | Purpose |
|------|---------|
| `HealthConnectManager.kt` | Health Connect permissions and data sync |

## Data Flow

### Fetching Workouts

1. User enters ESP32 address and taps "Connect"
2. `MainViewModel` creates/updates `ApiClient` with new URL
3. `ApiClient` calls `Esp32Api.getSessions()` via Retrofit
4. ESP32 returns list of session summaries
5. ViewModel updates `StateFlow<List<SessionSummary>>`
6. UI observes StateFlow and renders SessionCards

### Syncing to Health Connect

1. User taps "Sync" on a session
2. ViewModel calls `ApiClient.getSessionDetail(id)`
3. ESP32 returns full session with heart rate samples
4. ViewModel passes data to `HealthConnectManager.insertSession()`
5. HealthConnectManager creates Health Connect records:
   - ExerciseSessionRecord
   - DistanceRecord
   - TotalCaloriesBurnedRecord
   - HeartRateRecord (if samples available)
   - PowerRecord (if samples available)
6. On success, ViewModel marks session as synced via API

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 1.9+ |
| UI Framework | Jetpack Compose |
| Design System | Material 3 |
| HTTP Client | Retrofit 2.9 + OkHttp 4.12 |
| Async | Kotlin Coroutines + StateFlow |
| Architecture | ViewModel + AndroidViewModel |
| Health Data | Health Connect 1.1.0-alpha |
| Min SDK | Android 9 (API 28) |

## Connection Modes

The app supports two connection modes to the ESP32:

### Access Point Mode (Default)
- ESP32 creates its own WiFi network
- SSID: `CrivitRower` / Password: `rowing123`
- Default address: `192.168.4.1`

### Station Mode
- ESP32 joins your home WiFi network
- Access via mDNS: `rowing.local`
- Requires mDNS configuration on ESP32
