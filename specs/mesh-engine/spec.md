# Feature Specification: Mesh Engine

**Status**: Migrated  
**Spec References**: spec/02-architecture.md  
**Subsystem**: `engine/`

## User Scenarios & Testing

### User Story 1 - Engine Lifecycle Orchestration (Priority: P1)

MeshEngine wires all subsystems (crypto, routing, messaging, transfer, transport, power) and manages the overall lifecycle (start, stop, pause, resume).

**Independent Test**: `MeshEngineApiLifecycleTest` — start/stop/pause/resume state transitions

**Acceptance Scenarios**:

1. **Given** a configured MeshEngine, **When** start() is called, **Then** transport begins scanning/advertising, routing starts, all subsystems activate
2. **Given** a running engine, **When** stop() is called, **Then** all subsystems shut down cleanly
3. **Given** a running engine, **When** pause() is called, **Then** BLE radio pauses but state is preserved

### User Story 2 - Noise Handshake Management (Priority: P1)

Engine orchestrates Noise XX handshakes for new peer connections and maintains session state.

**Independent Test**: `MeshEngineTest` scenarios 1-6 — handshake initiation, completion, rejection

**Acceptance Scenarios**:

1. **Given** a new peer discovered via BLE, **When** connection established, **Then** Noise XX handshake is initiated
2. **Given** handshake completes successfully, **When** session is established, **Then** encrypted communication begins
3. **Given** TrustStore rejects remote key, **When** handshake fails, **Then** connection is closed with diagnostic

### User Story 3 - Pseudonym Rotation (Priority: P2)

HMAC-based pseudonyms rotate on epoch boundaries with deterministic stagger to prevent tracking.

**Independent Test**: `PseudonymRotatorTest`, `PseudonymVerificationTest`, `PseudonymRotationIntegrationTest`

### User Story 4 - Mesh State Management (Priority: P2)

Periodic sweeps manage peer timeouts, route expiry, and connection health.

**Independent Test**: `MeshStateManagerTest`

## Requirements

- **FR-001**: MeshEngine MUST wire all subsystems via constructor injection (no DI framework)
- **FR-002**: NoiseHandshakeManager MUST orchestrate 3-message XX flow per peer
- **FR-003**: PseudonymRotator MUST compute HMAC-SHA-256 pseudonyms on configurable epoch boundaries
- **FR-004**: Pseudonym stagger MUST be deterministic per-node (prevent simultaneous rotation)
- **FR-005**: MeshStateManager MUST sweep stale peers, expired routes, and unhealthy connections
- **FR-006**: PowerTierCodec MUST encode/decode power tier in advertisement payload
- **FR-007**: Engine MUST emit diagnostics for all handshake events and state transitions

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Start-to-first-advertisement | <500ms | Integration test |
| Handshake completion | <50ms (3 messages) | MeshEngineIntegrationTest |
| Pseudonym rotation | <1ms computation | PseudonymRotatorTest |

## Success Criteria

- **SC-001**: Full lifecycle (start→run→pause→resume→stop) works correctly
- **SC-002**: Handshakes succeed between peers in MeshTestHarness
- **SC-003**: Pseudonyms rotate on epoch boundaries with correct stagger
- **SC-004**: State sweeps clean up stale entries
- **SC-005**: 100% line and branch coverage
