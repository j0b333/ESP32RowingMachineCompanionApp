# ESP32 REST API

This document describes the REST API provided by the ESP32 rowing monitor for communication with the companion app.

## Base URL

The ESP32 exposes its API over HTTP:

| Mode | URL |
|------|-----|
| Access Point | `http://192.168.4.1` |
| Station (mDNS) | `http://rowing.local` |

## Endpoints

### GET /api/status

Returns the current device status and active workout information.

**Response:**
```json
{
  "online": true,
  "workoutActive": false,
  "distance": 0.0,
  "strokes": 0,
  "duration": 0,
  "heartRate": 0,
  "freeHeap": 125000,
  "bleConnected": false,
  "wsClients": 0
}
```

| Field | Type | Description |
|-------|------|-------------|
| `online` | Boolean | Device is responding |
| `workoutActive` | Boolean | Workout currently in progress |
| `distance` | Float | Current workout distance (meters) |
| `strokes` | Int | Current stroke count |
| `duration` | Int | Current workout duration (seconds) |
| `heartRate` | Int | Current heart rate (BPM) |
| `freeHeap` | Int | Free memory (bytes) |
| `bleConnected` | Boolean | Heart rate monitor connected |
| `wsClients` | Int | WebSocket clients connected |

---

### GET /api/sessions

Returns a list of all stored workout sessions.

**Response:**
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

| Field | Type | Unit | Description |
|-------|------|------|-------------|
| `id` | Int | - | Unique session identifier |
| `startTime` | Long | ms (Unix epoch) | Workout start timestamp |
| `duration` | Int | seconds | Total workout duration |
| `distance` | Float | meters | Total distance rowed |
| `strokes` | Int | count | Total stroke count |
| `calories` | Int | kcal | Total calories burned |
| `avgPower` | Float | watts | Average power output |
| `avgPace` | Float | sec/500m | Average pace |
| `avgHeartRate` | Int | BPM | Average heart rate |
| `maxHeartRate` | Int | BPM | Maximum heart rate |
| `synced` | Boolean | - | Has been synced to Health Connect |

---

### GET /api/sessions/{id}

Returns detailed session data including heart rate, power, and speed samples.

**Response:**
```json
{
  "id": 42,
  "startTime": 1706634000000,
  "duration": 1800,
  "distance": 5432.5,
  "calories": 387,
  "heartRateSamples": [
    { "time": 1706634001000, "bpm": 68 },
    { "time": 1706634002000, "bpm": 72 }
  ],
  "powerSamples": [
    { "time": 1706634001000, "watts": 145 },
    { "time": 1706634002000, "watts": 168 }
  ],
  "speedSamples": [
    { "time": 1706634001000, "metersPerSecond": 2.4 },
    { "time": 1706634002000, "metersPerSecond": 2.8 }
  ]
}
```

| Field | Type | Unit | Description |
|-------|------|------|-------------|
| `id` | Int | - | Session identifier |
| `startTime` | Long | ms (Unix epoch) | Workout start timestamp |
| `duration` | Int | seconds | Total workout duration |
| `distance` | Float | meters | Total distance rowed |
| `calories` | Int | kcal | Total calories burned |
| `heartRateSamples` | Array | - | Heart rate data points |
| `heartRateSamples[].time` | Long | ms (Unix epoch) | Sample timestamp |
| `heartRateSamples[].bpm` | Int | BPM | Heart rate value |
| `powerSamples` | Array | - | Power data points |
| `powerSamples[].time` | Long | ms (Unix epoch) | Sample timestamp |
| `powerSamples[].watts` | Float | watts | Power value |
| `speedSamples` | Array | - | Speed data points |
| `speedSamples[].time` | Long | ms (Unix epoch) | Sample timestamp |
| `speedSamples[].metersPerSecond` | Float | m/s | Speed value |

**Notes:**
- Sample arrays may be empty if data is not available
- Sample frequency can vary (per-stroke, per-second, or averaged intervals)

---

### POST /api/sessions/{id}/synced

Marks a session as synced to Health Connect.

**Request Body:** Empty

**Response:**
```json
{
  "success": true
}
```

---

### POST /workout/start

Starts a new workout session.

**Request Body:** Empty

**Response:**
```json
{
  "success": true
}
```

---

### POST /workout/stop

Stops the current workout session.

**Request Body:** Empty

**Response:**
```json
{
  "success": true
}
```

---

### GET /live

Returns real-time workout data (used for live display during workouts).

**Response:**
```json
{
  "distance": 1234.5,
  "strokes": 123,
  "duration": 300,
  "heartRate": 145,
  "power": 175.5,
  "pace": 115.0,
  "strokeRate": 24
}
```

## Error Handling

All endpoints return appropriate HTTP status codes:

| Code | Description |
|------|-------------|
| 200 | Success |
| 404 | Session not found |
| 500 | Internal server error |

Error responses include a message:
```json
{
  "error": "Session not found"
}
```

## Timeouts

Recommended client timeouts:
- Connect: 5 seconds
- Read: 30 seconds
- Write: 10 seconds
