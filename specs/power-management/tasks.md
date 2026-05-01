# Tasks: Power Management

**Status**: Migrated — task tracking reset; all actionable tasks pending

## Phase 1: Foundation

- [ ] T001 Define `PowerTier` enum (HIGH, NORMAL, LOW)
- [ ] T002 [P] Define `PowerConfig` (thresholds, intervals, connection limits)
- [ ] T003 [P] Define `PowerProfile` (per-tier scan/connection parameters)
- [ ] T004 [P] Define `BatteryMonitor` interface
- [ ] T005 [P] Implement `FixedBatteryMonitor` (test utility)
- [ ] T006 Write `FixedBatteryMonitorTest`

## Phase 2: Power Engine

- [ ] T007 Implement `PowerModeEngine` (tier state machine, hysteresis)
- [ ] T008 Implement `PowerManager` (orchestrator: battery poll → tier evaluation → policy enforcement)
- [ ] T009 [P] Implement `BleConnectionParameterPolicy` (BLE params per tier)
- [ ] T010 [P] Implement `ConnectionLimiter` (max connections per tier)
- [ ] T011 [P] Implement `TieredShedder` (shed connections under pressure)
- [ ] T012 [P] Define `ManagedConnection`, `PeerKey`, `TransferStatus` types
- [ ] T013 Write `PowerManagerTest`

## Phase 3: Graceful Drain

- [ ] T014 Implement `GracefulDrainManager` (complete in-flight transfers before disconnect)
- [ ] T015 Write `GracefulDrainIntegrationTest` (MeshTestHarness)

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify
```

All 15 actionable tasks are now pending.
