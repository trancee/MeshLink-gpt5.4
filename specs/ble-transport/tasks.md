# Tasks: BLE Transport

**Status**: Complete — transport foundation, discovery helpers, virtual transport, and platform scaffolding implemented

## Phase 1: Foundation

- [x] T001 Define `BleTransport` interface (connect, disconnect, send, receive, advertise)
- [x] T002 [P] Define `BleTransportConfig` (scan intervals, connection timeouts, advertisement settings)
- [x] T003 [P] Define `GattConstants` (service UUID, characteristic UUIDs)
- [x] T004 Write `BleTransportConfigTest`, `GattConstantsTest`

## Phase 2: L2CAP & Frame Codec

- [x] T005 Implement `L2capFrameCodec` (length-prefixed frame encoding/decoding)
- [x] T006 [P] Implement `L2capRetryScheduler` (exponential backoff with jitter)
- [x] T007 Write `L2capFrameCodecTest`
- [x] T008 Write `L2capRetrySchedulerTest`

## Phase 3: Discovery & Connection

- [x] T009 Implement `AdvertisementCodec` (pseudonym + power tier in BLE advertisement)
- [x] T010 [P] Implement `ConnectionInitiationPolicy` (deterministic initiator from peer IDs)
- [x] T011 [P] Implement `MeshHashFilter` (advertisement dedup)
- [x] T012 [P] Implement `OemL2capProbeCache` (device model → capability cache)
- [x] T013 [P] Implement `OemSlotTracker` (advertisement slot management)
- [x] T014 [P] Implement `WriteLatencyTracker`
- [x] T015 Write `AdvertisementCodecTest`, `ConnectionInitiationPolicyTest`
- [x] T016 Write `MeshHashFilterTest`, `OemL2capProbeCacheTest`, `OemSlotTrackerTest`
- [x] T017 Write `WriteLatencyTrackerTest`

## Phase 4: Platform Actuals

- [x] T018 [P] Implement `AndroidBleTransport` (BluetoothGatt, BluetoothLeScanner, L2CAP)
- [x] T019 [P] Implement `IosBleTransport` (CBCentralManager, CBPeripheralManager, CBL2CAPChannel)
- [x] T020 [P] Implement `MeshLinkService` (Android foreground service for BLE)
- [x] T021 [P] Implement `VirtualMeshTransport` (test double for integration tests)
- [x] T022 Write `VirtualMeshTransportTest`

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify
```

22 actionable tasks complete, 0 remain pending.
