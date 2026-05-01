# Tasks: BLE Transport

**Status**: In progress — transport foundation types and constants bootstrapped

## Phase 1: Foundation

- [x] T001 Define `BleTransport` interface (connect, disconnect, send, receive, advertise)
- [x] T002 [P] Define `BleTransportConfig` (scan intervals, connection timeouts, advertisement settings)
- [x] T003 [P] Define `GattConstants` (service UUID, characteristic UUIDs)
- [x] T004 Write `BleTransportConfigTest`, `GattConstantsTest`

## Phase 2: L2CAP & Frame Codec

- [x] T005 Implement `L2capFrameCodec` (length-prefixed frame encoding/decoding)
- [ ] T006 [P] Implement `L2capRetryScheduler` (exponential backoff with jitter)
- [x] T007 Write `L2capFrameCodecTest`
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

6 actionable tasks complete, 16 remain pending.
