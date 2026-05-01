# Feature Specification: Public API & Diagnostics

**Status**: Migrated  
**Spec References**: spec/11-diagnostics.md, spec/13-distribution-api-compliance.md  
**Subsystem**: `api/`

## Specification Ownership

This spec is the canonical source for:

- public API surface
- diagnostic catalog size and semantics
- cross-platform API parity requirements

If another spec references a conflicting public API shape, diagnostic code count, or platform API contract, this spec is canonical.

## User Scenarios & Testing

### User Story 1 - MeshLink API Surface (Priority: P1)

App developers interact with MeshLink through a single, platform-identical public API.

**Independent Test**: `MeshLinkTest`, `MeshLinkConfigTest`

**Acceptance Scenarios**:

1. **Given** an app developer, **When** they call `MeshLink.createAndroid(context, config)`, **Then** they get a fully-wired MeshLinkApi instance
2. **Given** a MeshLinkApi, **When** `start()` is called, **Then** BLE scanning/advertising begins and state transitions to RUNNING
3. **Given** a MeshLinkApi, **When** `send(peerId, payload)` is called, **Then** the message is encrypted and delivered via the mesh

### User Story 2 - Diagnostic Event System (Priority: P1)

26 diagnostic codes across 4 severity levels (DEBUG, INFO, WARN, ERROR) provide observability.

**Independent Test**: `DiagnosticSinkTest` — ring buffer, drop counting, lazy payload evaluation

**Acceptance Scenarios**:

1. **Given** a diagnostic event is emitted, **When** observed via `diagnosticEvents` Flow, **Then** the event contains code, level, timestamp, and typed payload
2. **Given** the ring buffer is full, **When** a new event arrives, **Then** oldest is dropped and drop counter increments
3. **Given** `redactPeerIds = true`, **When** events with peer IDs are emitted, **Then** peer IDs are truncated

### User Story 3 - Configuration DSL (Priority: P2)

Type-safe builder DSL for configuring MeshLink behavior.

**Independent Test**: `MeshLinkConfigTest` — builder validation, clamping, defaults

### User Story 4 - Lifecycle State Machine (Priority: P2)

Public state enum with well-defined transitions.

**Independent Test**: `MeshLinkStateTest` — all valid/invalid transitions

## Requirements

- **FR-001**: MeshLinkApi MUST expose: start, stop, pause, resume, send, peers, messages, diagnosticEvents
- **FR-002**: MeshLinkConfig DSL MUST validate all inputs and clamp to legal ranges
- **FR-003**: DiagnosticSink MUST use SharedFlow ring buffer with DROP_OLDEST
- **FR-004**: 26 DiagnosticCode values MUST cover all subsystem events
- **FR-005**: PeerState sealed hierarchy MUST represent all peer lifecycle states
- **FR-006**: RegulatoryRegion MUST clamp radio parameters to legal limits
- **FR-007**: TrustMode enum MUST expose TOFU, STRICT, PROMPT
- **FR-008**: API shape MUST be identical across Android, iOS, JVM targets (SKIE for Swift)

## Public API Surface

- `MeshLinkApi` — main interface
- `MeshLink` — companion factory
- `MeshLinkConfig` — configuration builder
- `MeshLinkState` — lifecycle enum (6 states)
- `PeerState` — peer lifecycle sealed class
- `PeerDetail` — peer information snapshot
- `PeerIdHex` — type-safe peer identifier
- `DiagnosticCode` — 26 event codes
- `DiagnosticPayload` — typed event payloads
- `TrustMode` — TOFU/STRICT/PROMPT
- `RegulatoryRegion` — radio parameter clamping
- `RoutingSnapshot` — routing table snapshot for debugging
- `ExperimentalMeshLinkApi` — opt-in annotation for unstable APIs

## Success Criteria

- **SC-001**: MeshLinkApi compiles identically on all targets (BCV apiCheck)
- **SC-002**: DiagnosticSink handles buffer pressure without blocking
- **SC-003**: Config DSL validates and clamps all parameters
- **SC-004**: State machine rejects invalid transitions
- **SC-005**: 100% line and branch coverage
