# Implementation Plan: Power Management

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/power-management/spec.md`  
**Status**: Migrated — implementation complete

## Summary

Three-tier power management (HIGH/NORMAL/LOW) that adapts BLE radio behavior, connection limits, and scan duty cycles to battery state. Includes graceful drain for low-battery shutdown.

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/power/
├── BatteryMonitor.kt              # Interface for battery level reporting
├── BleConnectionParameterPolicy.kt  # BLE params per power tier
├── ConnectionLimiter.kt           # Max concurrent connections per tier
├── FixedBatteryMonitor.kt         # Test/fixed-value battery monitor
├── GracefulDrainManager.kt        # Drain in-flight transfers before disconnect
├── ManagedConnection.kt           # Connection wrapper with power tracking
├── PeerKey.kt                     # Connection identity key
├── PowerConfig.kt                 # Thresholds, intervals, limits
├── PowerManager.kt                # Tier evaluation orchestrator
├── PowerModeEngine.kt             # Tier state machine
├── PowerProfile.kt                # Per-tier scan/connection parameters
├── PowerTier.kt                   # HIGH/NORMAL/LOW enum
├── TieredShedder.kt               # Connection shedding under pressure
└── TransferStatus.kt              # In-flight transfer state for drain
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Tier count | 3 (HIGH/NORMAL/LOW) | Covers common battery states without complexity |
| Battery source | BatteryMonitor interface | Platform actual; testable with FixedBatteryMonitor |
| Drain strategy | Complete in-flight, then disconnect | User experience over battery savings |
| Connection params | Per-tier BLE intervals | Directly controls radio power draw |
