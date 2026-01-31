# RowingSync

**Android companion app for the [ESP32 Rowing Machine](https://github.com/j0b333/ESP32RowingMachine)**

Sync your rowing workouts to Google Health Connect, Samsung Health, Google Fit, and other fitness apps.

## What It Does

1. **Connect** to your ESP32 rowing monitor via WiFi
2. **View** all your stored rowing sessions
3. **Sync** workouts to Health Connect with one tap

## Quick Start

1. Connect your phone to the ESP32's WiFi (`CrivitRower` / `rowing123`)
2. Open the app and enter `192.168.4.1`
3. Tap Connect to see your workouts
4. Tap Sync on any workout to save it to Health Connect

## Requirements

- Android 9+
- [Health Connect](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata) app installed

## Documentation

- [Companion App Guide](docs/COMPANION_APP.md) - Detailed app usage and features
- [System Architecture](docs/ARCHITECTURE.md) - Technical overview
- [ESP32 API Reference](docs/API.md) - REST API specification
- [Health Connect Integration](docs/HEALTH_CONNECT.md) - Health data sync details
- [Health Connect Data Spec](docs/HEALTH_CONNECT_DATA_SPEC.md) - ESP32 data format for Health Connect
- [Physics Model](docs/PHYSICS_MODEL.md) - Rowing metrics calculation

## Acknowledgments

The rowing physics model is based on the excellent work of the **[Open Rowing Monitor](https://github.com/laberning/openrowingmonitor)** project by laberning.

## License

MIT License - See [LICENSE](LICENSE) for details.
