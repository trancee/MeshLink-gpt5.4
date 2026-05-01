# Tasks: Public API & Diagnostics

**Status**: Complete — API surface, diagnostics, config DSL, factories, and stub integrations implemented

## Phase 1: Core Types

- [x] T001 Define `MeshLinkState` enum (UNINITIALIZED, RUNNING, PAUSED, STOPPED, RECOVERABLE, TERMINAL)
- [x] T002 [P] Define `PeerIdHex` value class (type-safe peer identifier)
- [x] T003 [P] Define `TrustMode` enum (TOFU, STRICT, PROMPT)
- [x] T004 [P] Define `RegulatoryRegion` enum with parameter clamping
- [x] T005 [P] Define `PeerState` sealed hierarchy
- [x] T006 [P] Define `PeerDetail` data class
- [x] T007 [P] Define `RoutingSnapshot` data class
- [x] T008 [P] Define `ExperimentalMeshLinkApi` opt-in annotation

## Phase 2: Diagnostic System

- [x] T009 Define `DiagnosticCode` enum (26 codes with severity levels)
- [x] T010 [P] Define `DiagnosticPayload` sealed hierarchy (typed payloads)
- [x] T011 Implement `DiagnosticSink` (SharedFlow ring buffer, DROP_OLDEST, lazy eval)
- [x] T012 [P] Implement `NoOpDiagnosticSink` (zero-overhead opt-out)
- [x] T013 Write `DiagnosticSinkTest`

## Phase 3: Configuration

- [x] T014 Implement `MeshLinkConfig` builder DSL (validation, clamping, sub-configs)
- [x] T015 Write `MeshLinkConfigTest` — validation, defaults, clamping
- [x] T016 Write `RegulatoryRegionClampingTest`

## Phase 4: API Interface & Factory

- [x] T017 Define `MeshLinkApi` interface (start, stop, pause, resume, send, peers, messages, diagnostics)
- [x] T018 Implement `MeshLink` companion (create factory, Flow mapping)
- [x] T019 Write `MeshLinkTest` — factory creation, Flow mapping
- [x] T020 Write `MeshLinkStateTest` — transition validation
- [x] T021 Write `PeerStateTest`, `PeerIdHexExtTest`

## Phase 5: Platform Factories

- [x] T022 [P] Implement `MeshLinkAndroidFactory` (androidMain)
- [x] T023 [P] Implement `MeshLinkIosFactory` (iosMain)

## Phase 6: Integration

- [x] T024 Write `StubApiWiringIntegrationTest`
- [x] T025 Write `PeerLifecycleIntegrationTest`

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:apiCheck
```

25 actionable tasks complete, 0 remain pending.
