# Tasks: Mesh Engine

**Status**: Migrated — task tracking reset; all actionable tasks pending

## Phase 1: Foundation

- [ ] T001 Define `MeshEngineConfig` (aggregate config for all subsystems)
- [ ] T002 [P] Define `HandshakeConfig` (timeout, retry settings)
- [ ] T003 [P] Implement `PowerTierCodec` (encode/decode power tier byte)
- [ ] T004 Write `PowerTierCodecTest`

## Phase 2: Pseudonym Rotation

- [ ] T005 Implement `PseudonymRotator` (HMAC-SHA-256, epoch boundaries, deterministic stagger)
- [ ] T006 Write `PseudonymRotatorTest` — epoch computation, stagger, rotation timer
- [ ] T007 Write `PseudonymVerificationTest` — ±1 epoch tolerance verification

## Phase 3: Handshake Management

- [ ] T008 Implement `NoiseHandshakeManager` (per-peer Noise XX orchestration)
- [ ] T009 Write `NoiseHandshakeManagerDiagnosticTest`

## Phase 4: State Management

- [ ] T010 Implement `MeshStateManager` (periodic sweep: stale peers, expired routes)
- [ ] T011 Write `MeshStateManagerTest`

## Phase 5: Engine Core

- [ ] T012 Implement `MeshEngine.create()` factory (wire all subsystems)
- [ ] T013 Implement lifecycle management (start, stop, pause, resume)
- [ ] T014 Implement inbound message routing (handshake vs data path)
- [ ] T015 Implement outbound message scheduling (via DeliveryPipeline)
- [ ] T016 Write `MeshEngineTest` — 9 scenarios covering all paths
- [ ] T017 Write `MeshEngineApiLifecycleTest`
- [ ] T018 Write `MeshEngineApiIdentityTest`

## Phase 6: Integration

- [ ] T019 Write `MeshEngineIntegrationTest` (multi-node via MeshTestHarness)
- [ ] T020 Write `PeerLifecycleIntegrationTest`
- [ ] T021 Write `PseudonymRotationIntegrationTest`
- [ ] T022 Write `StubApiWiringIntegrationTest`

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify
```

All 22 actionable tasks are now pending.
