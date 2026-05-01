# Tasks: Wire Format & Codec

**Status**: In progress — foundation and first Hello codec slice bootstrapped

## Phase 1: Foundation

- [x] T001 Define `MessageType` enum (1-byte tag for all message types)
- [x] T002 [P] Implement `ReadBuffer` (little-endian reader with bounds checking)
- [x] T003 [P] Implement `WriteBuffer` (little-endian writer with auto-grow)
- [x] T004 Define `ValidationResult` sealed type (Valid/Invalid)
- [x] T005 Write `ReadWriteBufferTest`

## Phase 2: Message Codecs

- [x] T006 [P] Implement `Hello` encode/decode
- [x] T007 [P] Implement `Handshake` encode/decode
- [ ] T008 [P] Implement `Update` encode/decode (Babel routing update)
- [ ] T009 [P] Implement `Chunk` encode/decode (transfer payload)
- [ ] T010 [P] Implement `ChunkAck` encode/decode (SACK)
- [ ] T011 [P] Implement `DeliveryAck` encode/decode
- [ ] T012 [P] Implement `Nack` encode/decode
- [x] T013 [P] Implement `Keepalive` encode/decode
- [ ] T014 [P] Implement `Broadcast` encode/decode
- [x] T015 [P] Implement `RoutedMessage` encode/decode
- [ ] T016 [P] Implement `ResumeRequest` encode/decode
- [ ] T017 [P] Implement `RotationAnnouncementMessage` encode/decode

## Phase 3: Codec Dispatcher & Validation

- [ ] T018 Implement `WireCodec` (encode/decode dispatcher by MessageType)
- [ ] T019 Implement `InboundValidator` (length bounds, hop limits, field constraints)
- [ ] T020 Write `WireFormatTest` — round-trip all message types
- [ ] T021 Write `InboundValidatorTest` — boundary conditions

## Phase 4: Benchmark

- [ ] T022 Add `WireFormatBenchmark` in jvmMain — encode+decode throughput

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:jvmBenchmark
```

9 actionable tasks complete, 13 remain pending. Current JVM and Android host-test coverage verify at 100%.
