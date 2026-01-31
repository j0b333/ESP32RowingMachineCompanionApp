# Health Connect Integration

This document describes how RowingSync integrates with Google Health Connect to sync rowing workout data.

## Overview

[Google Health Connect](https://developer.android.com/health-connect) is a platform that centralizes health and fitness data on Android devices. RowingSync uses Health Connect to:

1. Store rowing workout sessions
2. Sync data to other fitness apps (Samsung Health, Google Fit, etc.)
3. Provide a unified view of fitness data across apps

## Required Permissions

The app requests these Health Connect permissions:

| Permission | Purpose |
|------------|---------|
| Read/Write Exercise Sessions | Store rowing workout records |
| Read/Write Heart Rate | Sync heart rate samples |
| Read/Write Distance | Store distance covered |
| Read/Write Calories Burned | Store calories burned |
| Read/Write Power | Store power (watts) samples |

## Record Types

### ExerciseSessionRecord

The main workout record that contains the exercise session metadata.

| Field | Value | Description |
|-------|-------|-------------|
| `exerciseType` | `EXERCISE_TYPE_ROWING_MACHINE` (73) | Identifies as rowing |
| `title` | "Rowing Session" | Display name |
| `startTime` | Session start | From ESP32 `startTime` |
| `endTime` | Session end | Calculated: `startTime + (duration * 1000)` |

### DistanceRecord

Total distance covered during the workout.

| Field | Source | Unit |
|-------|--------|------|
| `distance` | ESP32 `distance` | Meters |
| `startTime` | Session start | Milliseconds |
| `endTime` | Session end | Milliseconds |

### TotalCaloriesBurnedRecord

Total calories burned during the workout.

| Field | Source | Unit |
|-------|--------|------|
| `energy` | ESP32 `calories` | Kilocalories |
| `startTime` | Session start | Milliseconds |
| `endTime` | Session end | Milliseconds |

### HeartRateRecord

Per-second heart rate samples (optional - only created if samples exist).

| Field | Source | Unit |
|-------|--------|------|
| `samples[].time` | ESP32 `heartRateSamples[].time` | Milliseconds |
| `samples[].beatsPerMinute` | ESP32 `heartRateSamples[].bpm` | BPM |

### PowerRecord

Power samples during the workout (optional - only created if samples exist).

| Field | Source | Unit |
|-------|--------|------|
| `samples[].time` | ESP32 `powerSamples[].time` | Milliseconds |
| `samples[].power` | ESP32 `powerSamples[].watts` | Watts |

### SpeedRecord

Speed samples during the workout (optional - only created if samples exist).

| Field | Source | Unit |
|-------|--------|------|
| `samples[].time` | ESP32 `speedSamples[].time` | Milliseconds |
| `samples[].speed` | ESP32 `speedSamples[].metersPerSecond` | m/s |

## Data Not Synced

Some ESP32 data is displayed in the app but not synced to Health Connect:

| Data | Reason |
|------|--------|
| Stroke count | No Health Connect equivalent |
| Stroke rate | No Health Connect equivalent |
| Drag factor | No Health Connect equivalent |
| Average values | Calculated from samples |

## Implementation

The `HealthConnectManager` class handles all Health Connect operations:

```kotlin
class HealthConnectManager(context: Context) {
    // Check if Health Connect is available
    suspend fun isAvailable(): Boolean
    
    // Request permissions from user
    suspend fun requestPermissions(activity: Activity)
    
    // Check if all permissions are granted
    suspend fun hasAllPermissions(): Boolean
    
    // Insert a rowing session with all records
    suspend fun insertSession(session: SessionDetail): Boolean
}
```

## Sync Flow

```
┌─────────────────┐
│  User taps Sync │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Check permissions│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Fetch full      │
│ session details │
│ from ESP32      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Create Health   │
│ Connect records │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Insert records  │
│ via Health      │
│ Connect API     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Mark session    │
│ as synced on    │
│ ESP32           │
└─────────────────┘
```

## Requirements

- **Android 9+** (API 28+)
- **Health Connect app** installed
  - Built-in on Android 14+
  - Available on Play Store for Android 9-13

## Troubleshooting

### Health Connect Not Available
- Ensure Health Connect app is installed
- For Android 9-13, download from Play Store
- For Android 14+, it's built into the system

### Permissions Denied
- Open Health Connect app
- Go to "App permissions"
- Find RowingSync and enable all permissions

### Sync Fails
- Check ESP32 connection
- Ensure workout has required data (start time, duration, distance, calories)
- Check Health Connect app for storage issues
