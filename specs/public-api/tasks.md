# Tasks: Public API & Diagnostics

**Status**: Migrated — task tracking reset; all actionable tasks pending

## Phase 1: Core Types

- [x] T001 Define `MeshLinkState` enum (UNINITIALIZED, RUNNING, PAUSED, STOPPED, RECOVERABLE, TERMINAL)
- [x] T002 [P] Define `PeerIdHex` value class (type-safe peer identifier)
- [x] T003 [P] Define `TrustMode` enum (TOFU, STRICT, PROMPT)
- [x] T004 [P] Define `RegulatoryRegion` enum with parameter clamping
- [ ] T005 [P] Define `PeerState` sealed hierarchy
- [ ] T006 [P] Define `PeerDetail` data class
- [ ] T007 [P] Define `RoutingSnapshot` data class
- [ ] T008 [P] Define `ExperimentalMeshLinkApi` opt-in annotation

## Phase 2: Diagnostic System

- [ ] T009 Define `DiagnosticCode` enum (26 codes with severity levels)
- [ ] T010 [P] Define `DiagnosticPayload` sealed hierarchy (typed payloads)
- [ ] T011 Implement `DiagnosticSink` (SharedFlow ring buffer, DROP_OLDEST, lazy eval)
- [ ] T012 [P] Implement `NoOpDiagnosticSink` (zero-overhead opt-out)
- [ ] T013 Write `DiagnosticSinkTest`

## Phase 3: Configuration

- [ ] T014 Implement `MeshLinkConfig` builder DSL (validation, clamping, sub-configs)
- [ ] T015 Write `MeshLinkConfigTest` — validation, defaults, clamping
- [x] T016 Write `RegulatoryRegionClampingTest`

## Phase 4: API Interface & Factory

- [ ] T017 Define `MeshLinkApi` interface (start, stop, pause, resume, send, peers, messages, diagnostics)
- [ ] T018 Implement `MeshLink` companion (create factory, Flow mapping)
- [ ] T019 Write `MeshLinkTest` — factory creation, Flow mapping
- [x] T020 Write `MeshLinkStateTest` — transition validation
- [ ] T021 Write `PeerStateTest`, `PeerIdHexExtTest`

## Phase 5: Platform Factories

- [ ] T022 [P] Implement `MeshLinkAndroidFactory` (androidMain)
- [ ] T023 [P] Implement `MeshLinkIosFactory` (iosMain)

## Phase 6: Integration

- [ ] T024 Write `StubApiWiringIntegrationTest`
- [ ] T025 Write `PeerLifecycleIntegrationTest`

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:apiCheck
```

6 actionable tasks complete, 19 remain pending.
