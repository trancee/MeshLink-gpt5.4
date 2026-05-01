# Tasks: BLE Transport

**Status**: Migrated — task tracking reset; all actionable tasks pending

## Phase 1: Foundation

- [ ] T001 Define `BleTransport` interface (connect, disconnect, send, receive, advertise)
- [ ] T002 [P] Define `BleTransportConfig` (scan intervals, connection timeouts, advertisement settings)
- [ ] T003 [P] Define `GattConstants` (service UUID, characteristic UUIDs)
- [ ] T004 Write `BleTransportConfigTest`, `GattConstantsTest`

## Phase 2: L2CAP & Frame Codec

- [ ] T005 Implement `L2capFrameCodec` (length-prefixed frame encoding/decoding)
- [ ] T006 [P] Implement `L2capRetryScheduler` (exponential backoff with jitter)
- [ ] T007 Write `L2capFrameCodecTest`
- [ ] T008 Write `L2capRetrySchedulerTest`

## Phase 3: Discovery & Connection

- [ ] T009 Implement `AdvertisementCodec` (pseudonym + power tier in BLE advertisement)
- [ ] T010 [P] Implement `ConnectionInitiationPolicy` (deterministic initiator from peer IDs)
- [ ] T011 [P] Implement `MeshHashFilter` (advertisement dedup)
- [ ] T012 [P] Implement `OemL2capProbeCache` (device model → capability cache)
- [ ] T013 [P] Implement `OemSlotTracker` (advertisement slot management)
- [ ] T014 [P] Implement `WriteLatencyTracker`
- [ ] T015 Write `AdvertisementCodecTest`, `ConnectionInitiationPolicyTest`
- [ ] T016 Write `MeshHashFilterTest`, `OemL2capProbeCacheTest`, `OemSlotTrackerTest`
- [ ] T017 Write `WriteLatencyTrackerTest`

## Phase 4: Platform Actuals

- [ ] T018 [P] Implement `AndroidBleTransport` (BluetoothGatt, BluetoothLeScanner, L2CAP)
- [ ] T019 [P] Implement `IosBleTransport` (CBCentralManager, CBPeripheralManager, CBL2CAPChannel)
- [ ] T020 [P] Implement `MeshLinkService` (Android foreground service for BLE)
- [ ] T021 [P] Implement `VirtualMeshTransport` (test double for integration tests)
- [ ] T022 Write `VirtualMeshTransportTest`

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify
```

All 22 actionable tasks are now pending.
