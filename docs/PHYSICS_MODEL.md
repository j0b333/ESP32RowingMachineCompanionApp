# Rowing Physics Model

This document describes the physics model used by the ESP32 rowing monitor to calculate rowing metrics.

## Overview

The ESP32 rowing monitor uses a physics-based model to calculate key rowing metrics from the flywheel rotation. This model is based on the work of the **[Open Rowing Monitor](https://github.com/laberning/openrowingmonitor)** project, an open-source rowing computer.

## How It Works

### Flywheel Physics

A rowing machine uses a flywheel that the rower accelerates during the drive phase and decelerates during the recovery phase. By measuring the flywheel's rotation, we can calculate:

1. **Power** (watts) - The rate at which energy is applied
2. **Pace** (seconds per 500m) - Standardized rowing speed metric
3. **Distance** (meters) - Total distance "rowed"
4. **Calories** - Energy expenditure estimate

### Key Equations

#### Drag Factor
The drag factor (k) characterizes the air resistance on the flywheel:

```
k = I × (ω₁ - ω₂) / Δt
```

Where:
- `I` = Moment of inertia of the flywheel
- `ω₁` = Angular velocity at start of recovery
- `ω₂` = Angular velocity at end of recovery
- `Δt` = Time of recovery phase

#### Power Calculation
Power is calculated from the energy transferred to the flywheel:

```
P = k × ω³
```

Where:
- `P` = Power in watts
- `k` = Drag factor
- `ω` = Angular velocity

#### Pace Calculation
Pace is derived from power using the standard rowing formula:

```
pace = (2.8 / P)^(1/3) × 500
```

Where:
- `pace` = Seconds per 500 meters
- `P` = Power in watts

#### Distance Calculation
Distance is calculated by integrating the "virtual boat speed":

```
v = (P / 2.8)^(1/3)
distance = Σ(v × Δt)
```

## Calibration

The drag factor is automatically calibrated during each recovery phase. This accounts for:
- Damper setting changes
- Air density variations (temperature, altitude)
- Machine-specific characteristics

## Data Samples

The ESP32 records per-stroke samples for:
- **Power** (watts) - Instantaneous power output
- **Speed** (m/s) - Virtual boat speed
- **Heart Rate** (BPM) - From connected Bluetooth heart rate monitor

These samples are stored with millisecond timestamps and synced to Health Connect.

## Accuracy

The physics model provides accuracy comparable to commercial rowing computers like the Concept2 PM5. Factors affecting accuracy:
- Sensor placement and quality
- Flywheel moment of inertia calibration
- Drag factor calculation precision

## References

### Open Rowing Monitor
This physics model is based on the excellent work of the **[Open Rowing Monitor](https://github.com/laberning/openrowingmonitor)** project by laberning. Open Rowing Monitor is an open-source, Raspberry Pi-based rowing computer that provides professional-grade metrics.

**Attribution:**
- Physics model concepts: [Open Rowing Monitor](https://github.com/laberning/openrowingmonitor)
- License: GPL-3.0

### Additional Resources
- [Physics of Ergometer Rowing](https://www.c2forum.com/viewtopic.php?f=2&t=1049) - Concept2 forum discussion
- [Rowing Machine Physics](https://www.physicsforums.com/threads/physics-of-rowing-machines.531851/) - Physics Forums
- [Concept2 Pace Calculator](https://www.concept2.com/indoor-rowers/training/calculators/watts-calculator) - Official watts/pace conversion

## Implementation Notes

The companion app receives pre-calculated metrics from the ESP32:
- The app does **not** perform physics calculations
- All power, pace, and distance values come from the ESP32
- The app focuses on data display and Health Connect synchronization

For implementation details of the physics model, see the [ESP32RowingMachine](https://github.com/j0b333/ESP32RowingMachine) repository.
