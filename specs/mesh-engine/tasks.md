# Tasks: Mesh Engine

**Status**: In progress — engine core and integrations implemented; outbound scheduling remains pending on DeliveryPipeline wiring

## Phase 1: Foundation

- [x] T001 Define `MeshEngineConfig` (aggregate config for all subsystems)
- [x] T002 [P] Define `HandshakeConfig` (timeout, retry settings)
- [x] T003 [P] Implement `PowerTierCodec` (encode/decode power tier byte)
- [x] T004 Write `PowerTierCodecTest`

## Phase 2: Pseudonym Rotation

- [x] T005 Implement `PseudonymRotator` (HMAC-SHA-256, epoch boundaries, deterministic stagger)
- [x] T006 Write `PseudonymRotatorTest` — epoch computation, stagger, rotation timer
- [x] T007 Write `PseudonymVerificationTest` — ±1 epoch tolerance verification

## Phase 3: Handshake Management

- [x] T008 Implement `NoiseHandshakeManager` (per-peer Noise XX orchestration)
- [x] T009 Write `NoiseHandshakeManagerDiagnosticTest`

## Phase 4: State Management

- [x] T010 Implement `MeshStateManager` (periodic sweep: stale peers, expired routes)
- [x] T011 Write `MeshStateManagerTest`

## Phase 5: Engine Core

- [x] T012 Implement `MeshEngine.create()` factory (wire all subsystems)
- [x] T013 Implement lifecycle management (start, stop, pause, resume)
- [x] T014 Implement inbound message routing (handshake vs data path)
- [ ] T015 Implement outbound message scheduling (via DeliveryPipeline)
- [x] T016 Write `MeshEngineTest` — 9 scenarios covering all paths
- [x] T017 Write `MeshEngineApiLifecycleTest`
- [x] T018 Write `MeshEngineApiIdentityTest`

## Phase 6: Integration

- [x] T019 Write `MeshEngineIntegrationTest` (multi-node via MeshTestHarness)
- [x] T020 Write `PeerLifecycleIntegrationTest`
- [x] T021 Write `PseudonymRotationIntegrationTest`
- [x] T022 Write `StubApiWiringIntegrationTest`

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify
```

21 actionable tasks complete, 1 remain pending.
