# Health Connect Data Specification for ESP32 Rowing Machine

This document specifies exactly what data the ESP32 API should provide for the companion app to sync to Google Health Connect. Only the fields listed here are needed - no extras required.

## Summary

The companion app uploads the following record types to Health Connect:

| Record Type | Required | Description |
|-------------|----------|-------------|
| ExerciseSessionRecord | ✅ Yes | The main workout record |
| DistanceRecord | ✅ Yes | Total distance covered |
| TotalCaloriesBurnedRecord | ✅ Yes | Calories burned |
| HeartRateRecord | ⚡ Optional | Per-second heart rate samples |

---

## Required API Endpoint Response

The ESP32 should provide a session detail endpoint (e.g., `GET /api/sessions/{id}`) with the following JSON structure:

```json
{
  "id": 1,
  "startTime": 1706634000000,
  "duration": 1800,
  "distance": 5000.0,
  "calories": 350,
  "heartRateSamples": [
    { "time": 1706634001000, "bpm": 72 },
    { "time": 1706634002000, "bpm": 75 },
    { "time": 1706634003000, "bpm": 78 }
  ]
}
```

---

## Field Specifications

### 1. ExerciseSessionRecord (Required)

The main workout session record.

| Field | Type | Unit | Description | ESP32 API Field |
|-------|------|------|-------------|-----------------|
| startTime | Long | Milliseconds (Unix epoch) | When the workout started | `startTime` |
| duration | Int | Seconds | Total workout duration | `duration` |
| exerciseType | Constant | - | Always `EXERCISE_TYPE_ROWING_MACHINE` (73) | N/A (set by app) |
| title | String | - | Optional title (app sets "Rowing Session") | N/A (set by app) |

**ESP32 must provide:**
```json
{
  "startTime": 1706634000000,
  "duration": 1800
}
```

**Notes:**
- `startTime` must be Unix epoch time in **milliseconds**
- `duration` is in **seconds**
- The app calculates `endTime = startTime + (duration * 1000)`

---

### 2. DistanceRecord (Required)

Total distance covered during the workout.

| Field | Type | Unit | Description | ESP32 API Field |
|-------|------|------|-------------|-----------------|
| distance | Float | Meters | Total distance rowed | `distance` |
| startTime | Long | Milliseconds | Same as session start | `startTime` |
| endTime | Long | Milliseconds | Calculated from duration | Calculated |

**ESP32 must provide:**
```json
{
  "distance": 5000.0
}
```

**Notes:**
- Distance should be in **meters** as a floating point number
- Example: 5km = `5000.0`

---

### 3. TotalCaloriesBurnedRecord (Required)

Calories burned during the workout.

| Field | Type | Unit | Description | ESP32 API Field |
|-------|------|------|-------------|-----------------|
| energy | Int | Kilocalories (kcal) | Total calories burned | `calories` |
| startTime | Long | Milliseconds | Same as session start | `startTime` |
| endTime | Long | Milliseconds | Calculated from duration | Calculated |

**ESP32 must provide:**
```json
{
  "calories": 350
}
```

**Notes:**
- Calories should be in **kilocalories (kcal)** as an integer
- This is what users commonly refer to as "calories"

---

### 4. HeartRateRecord (Optional - Data Optional, Field Required)

Per-second heart rate samples during the workout. The **HeartRateRecord is only created if samples are provided**. However, the `heartRateSamples` field must always be present in the API response (can be an empty array).

| Field | Type | Unit | Description | ESP32 API Field |
|-------|------|------|-------------|-----------------|
| samples | Array | - | List of heart rate samples | `heartRateSamples` |
| samples[].time | Long | Milliseconds (Unix epoch) | Timestamp of this sample | `heartRateSamples[].time` |
| samples[].bpm | Int | Beats per minute | Heart rate at this moment | `heartRateSamples[].bpm` |

**ESP32 must provide (if heart rate available):**
```json
{
  "heartRateSamples": [
    { "time": 1706634001000, "bpm": 72 },
    { "time": 1706634002000, "bpm": 75 },
    { "time": 1706634003000, "bpm": 78 }
  ]
}
```

**Notes:**
- Each sample `time` must be Unix epoch time in **milliseconds**
- `bpm` should be a positive integer (typically 40-220)
- Samples should be within the workout time range (between `startTime` and `endTime`)
- **If no heart rate monitor is connected, return an empty array: `"heartRateSamples": []`**

---

## Complete Example API Response

Here's a complete example of what the ESP32 `/api/sessions/{id}` endpoint should return:

```json
{
  "id": 42,
  "startTime": 1706634000000,
  "duration": 1800,
  "distance": 5432.5,
  "calories": 387,
  "heartRateSamples": [
    { "time": 1706634001000, "bpm": 68 },
    { "time": 1706634002000, "bpm": 72 },
    { "time": 1706634003000, "bpm": 75 },
    { "time": 1706634060000, "bpm": 142 },
    { "time": 1706634120000, "bpm": 156 },
    { "time": 1706635800000, "bpm": 165 }
  ]
}
```

---

## Data NOT Used by Health Connect

The following fields from the ESP32 API are **NOT uploaded to Health Connect** (but may be displayed in the app):

| Field | Description | Why Not Used |
|-------|-------------|--------------|
| `strokes` | Total stroke count | No Health Connect equivalent |
| `avgPower` | Average power (watts) | Could be added via PowerRecord* |
| `avgPace` | Average pace (sec/500m) | No direct equivalent |
| `avgHeartRate` | Average heart rate | Calculated from samples |
| `maxHeartRate` | Maximum heart rate | Calculated from samples |
| `dragFactor` | Rowing machine drag factor | No Health Connect equivalent |
| `synced` | Sync status flag | Internal tracking only |

*Note: Health Connect does support `PowerRecord` for per-second power samples. If you want to add power data in the future, the ESP32 could provide:
```json
{
  "powerSamples": [
    { "time": 1706634001000, "watts": 150 },
    { "time": 1706634002000, "watts": 165 }
  ]
}
```

---

## Minimum Required Fields

**Absolute minimum** the ESP32 must provide:

```json
{
  "id": 1,
  "startTime": 1706634000000,
  "duration": 1800,
  "distance": 5000.0,
  "calories": 350,
  "heartRateSamples": []
}
```

| Field | Required | Type | Unit |
|-------|----------|------|------|
| `id` | ✅ | Int | - |
| `startTime` | ✅ | Long | Milliseconds (Unix epoch) |
| `duration` | ✅ | Int | Seconds |
| `distance` | ✅ | Float | Meters |
| `calories` | ✅ | Int | Kilocalories |
| `heartRateSamples` | ✅ | Array | (can be empty if no HR monitor) |

---

## Session List Endpoint

For the session list endpoint (`GET /api/sessions`), the ESP32 should return:

```json
{
  "sessions": [
    {
      "id": 1,
      "startTime": 1706634000000,
      "duration": 1800,
      "distance": 5000.0,
      "strokes": 450,
      "calories": 350,
      "avgPower": 165.5,
      "avgPace": 120.0,
      "avgHeartRate": 142,
      "maxHeartRate": 168,
      "synced": false
    }
  ]
}
```

The `synced` field should be set to `true` when the app calls `POST /api/sessions/{id}/synced`.

---

## Summary Table

| Data | Health Connect Record | ESP32 Field | Format |
|------|----------------------|-------------|--------|
| Workout timing | ExerciseSessionRecord | `startTime`, `duration` | ms, seconds |
| Distance | DistanceRecord | `distance` | meters (float) |
| Calories | TotalCaloriesBurnedRecord | `calories` | kcal (int) |
| Heart rate | HeartRateRecord | `heartRateSamples[]` | time (ms) + bpm (int) |

---

## Future Expansion (Optional)

If you want to add more data to Health Connect in the future, here are additional record types that could be relevant for rowing:

| Record Type | Data | ESP32 Field Needed |
|-------------|------|-------------------|
| PowerRecord | Per-second power | `powerSamples[].time`, `powerSamples[].watts` |
| SpeedRecord | Per-second speed | `speedSamples[].time`, `speedSamples[].metersPerSecond` |
| StepsRecord | Stroke count (as steps) | Not recommended - misleading |

For now, focus on the 4 core record types documented above.
