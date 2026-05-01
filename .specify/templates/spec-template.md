# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`  
**Created**: [DATE]  
**Status**: Draft  
**Spec References**: [List relevant spec/ documents, e.g., spec/05-security-encryption.md]  
**Input**: User description: "$ARGUMENTS"

## User Scenarios & Testing *(mandatory)*

<!--
  User stories MUST be prioritized as independently testable vertical slices.
  Each story should be implementable and verifiable via MeshTestHarness +
  VirtualMeshTransport without real BLE hardware.
  
  Assign priorities (P1, P2, P3). P1 = minimum viable feature.
-->

### User Story 1 - [Brief Title] (Priority: P1)

[Describe this user journey — who is the consumer (app developer integrating MeshLink, or internal subsystem)?]

**Why this priority**: [Value justification]

**Independent Test**: [How this is verified via VirtualMeshTransport / unit test / benchmark]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Description]

**Why this priority**: [Value justification]

**Independent Test**: [Verification approach]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 3 - [Brief Title] (Priority: P3)

[Description]

**Why this priority**: [Value justification]

**Independent Test**: [Verification approach]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### Edge Cases

- What happens when [boundary condition]?
- How does the system handle [error scenario]?
- What is the behavior under [resource constraint — memory, battery, connection loss]?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST [specific capability]
- **FR-002**: System MUST [specific capability]
- **FR-003**: System MUST [specific capability]

*Mark unclear requirements explicitly:*
- **FR-00X**: System MUST [NEEDS CLARIFICATION: detail missing]

### Key Entities *(include if feature involves data/state)*

- **[Entity 1]**: [What it represents, key attributes, which package it belongs to]
- **[Entity 2]**: [Relationships to existing entities in the codebase]

## Wire Protocol Impact *(include if feature adds/modifies messages)*

<!--
  Per constitution: wire protocol stability is non-negotiable.
  Any new MessageType or field change must be documented here.
-->

- **New message type(s)**: [e.g., `MessageType.FOO` — describe payload structure]
- **Modified messages**: [e.g., added field X to existing Chunk message]
- **Backward compatibility**: [How older nodes handle the new message — ignored? version gate?]
- **Wire codec changes**: [Files affected in `wire/` and `wire/messages/`]
- **FlatBuffers schema**: [Changes to `spec/schemas/meshlink.fbs` if any]

> If no wire protocol impact, state: "No wire protocol changes required."

## Platform Actuals *(include if feature requires platform-specific code)*

<!--
  Per constitution: all shared logic in commonMain. Platform source sets
  contain ONLY actual implementations and platform glue.
-->

| Platform | Source Set | Implementation Notes |
|----------|-----------|---------------------|
| Common | `commonMain` | [Shared interface/logic] |
| Android | `androidMain` | [Android-specific actual — e.g., BluetoothManager API] |
| iOS | `iosMain` | [iOS-specific actual — e.g., CoreBluetooth cinterop] |
| JVM | `jvmMain` | [Test infrastructure shim — if needed] |

> If purely commonMain, state: "No platform actuals required."

## Security Analysis *(mandatory for crypto/trust/transport features)*

<!--
  Per constitution: no third-party crypto, all ops via CryptoProvider,
  validated against Wycheproof vectors.
-->

- **Threat model**: [What attacks does this feature defend against?]
- **Crypto primitives used**: [e.g., ChaCha20-Poly1305 AEAD, X25519 DH, Ed25519 signing]
- **Key material handling**: [How keys are derived, stored, rotated, destroyed]
- **Wycheproof coverage**: [Which test vector files validate this feature's crypto]

> If no security impact, state: "No cryptographic operations introduced."

## Performance Budget *(mandatory)*

<!--
  Per constitution: quantified performance targets. Every feature must
  declare its budget and how it will be measured.
-->

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Latency | [e.g., <1ms per operation] | [benchmark name or test approach] |
| Memory | [e.g., +0 allocations in hot path] | [how verified] |
| Throughput | [e.g., must not reduce L2CAP throughput by >5%] | [benchmark] |
| Battery | [e.g., no additional wake cycles in LOW tier] | [how verified] |

## Public API Surface *(include if feature modifies MeshLinkApi)*

<!--
  Per constitution: explicitApi() mode, BCV tracking, identical shape across targets.
-->

- **New public types**: [List classes/interfaces/enums added to `api/` package]
- **Modified signatures**: [Existing API changes — triggers BCV diff review]
- **Deprecations**: [Any APIs being deprecated with migration path]
- **SKIE impact**: [How iOS Swift consumers will see this — AsyncStream, enum, etc.]

> If internal-only, state: "No public API surface changes."

## Diagnostic Events *(include if feature emits diagnostics)*

<!--
  Per constitution: 26 codes, 3 severity tiers, identical on all platforms.
  Adding a new code requires listing it here.
-->

- **New codes**: [e.g., `DiagnosticCode.FOO_FAILED` at WARNING level]
- **Payload**: [What `DiagnosticPayload` fields are included]
- **Existing code reuse**: [Which existing codes this feature emits]

> If no diagnostics, state: "No new diagnostic codes."

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [e.g., "All acceptance scenarios pass via VirtualMeshTransport"]
- **SC-002**: [e.g., "100% line and branch coverage maintained (koverVerify green)"]
- **SC-003**: [e.g., "Benchmark shows <10% regression from baseline"]
- **SC-004**: [e.g., "BCV apiCheck passes (no unintentional API breaks)"]

## Assumptions

- [Assumption about scope — e.g., "Only single-hop delivery; multi-hop deferred to M00X"]
- [Assumption about platform — e.g., "Android API 29+ only; no backport needed"]
- [Dependency — e.g., "Requires CryptoProvider.hkdf() from M001"]

## Subsystem Placement

<!--
  Map this feature to the existing package structure.
  Reference: api/, crypto/, crypto/noise/, engine/, messaging/, power/,
  routing/, storage/, testing/, transfer/, transport/, util/, wire/, wire/messages/
-->

- **Primary package**: `ch.trancee.meshlink.[subsystem]`
- **Touches**: [List other packages this feature interacts with]
- **New package**: [If creating a new sub-package, justify why]
