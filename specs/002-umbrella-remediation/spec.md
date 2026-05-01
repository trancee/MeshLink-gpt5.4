# Feature Specification: Umbrella Remediation Wave 1

**Feature Branch**: `002-umbrella-remediation`  
**Created**: 2026-05-01  
**Status**: Draft  
**Spec References**: `specs/001-codebase-spec-analysis/spec.md`, `specs/ble-transport/spec.md`, `specs/crypto-noise-protocol/spec.md`, `specs/mesh-engine/spec.md`, `specs/babel-routing/spec.md`, `specs/messaging-delivery/spec.md`, `specs/sack-transfer/spec.md`, `specs/power-management/spec.md`, `specs/public-api/spec.md`, `specs/platform-distribution/spec.md`, `specs/wire-format/spec.md`  
**Input**: User description: "plan all as umbrella remediation feature"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Real Device Transport and Secure Session Establishment (Priority: P1)

As a MeshLink integrator, I need the Android and iOS platform implementations to use real BLE transport and real Noise session establishment instead of test doubles so that device builds behave like the product described by the subsystem specs.

**Why this priority**: The current codebase contains the common transport/crypto scaffolding, but the platform actuals still delegate to virtual transports and the Noise XX flow is only a role/round controller. Until this is corrected, downstream runtime behavior is necessarily incomplete.

**Independent Test**: Android and iOS device-path integration tests establish a connection, complete a real Noise XX handshake, derive real cipher state, and exchange encrypted payloads through the platform transport abstraction.

**Acceptance Scenarios**:

1. **Given** two physical devices running the same MeshLink app ID, **When** they start the library, **Then** discovery and connection proceed through real platform BLE code rather than `VirtualMeshTransport` delegation.
2. **Given** a newly discovered peer, **When** the three-message Noise XX exchange completes, **Then** both sides derive matching transport sessions and TrustStore policy is consulted before the session is accepted.
3. **Given** L2CAP is unavailable on a device, **When** the transport layer attempts the preferred data path, **Then** the implementation falls back to GATT while preserving the shared `BleTransport` contract.

---

### User Story 2 - Runtime Gap Closure Across Routing, Messaging, Transfers, and Operations (Priority: P1)

As a MeshLink integrator, I need the shared runtime to close the remaining product-level gaps in routing verification, message delivery, large transfer recovery, buffering, observability, erasure, and mesh isolation so that the implementation matches the current spec set end-to-end.

**Why this priority**: The repository now has strong subsystem building blocks, but several cross-cutting capabilities are still partial or missing, especially where subsystems meet: routed encrypted send paths, cut-through relay semantics, retransmission behavior, health state exposure, and destructive state-management APIs.

**Independent Test**: Canonical `MeshTestHarness` + `VirtualMeshTransport` integration tests cover multi-hop convergence, cut-through relay, transfer resume/retransmission, store-and-forward buffering, health snapshot correctness, and GDPR-style forget/reset behavior.

**Acceptance Scenarios**:

1. **Given** a 10-node topology change, **When** the routing subsystem recalculates paths, **Then** convergence completes within the spec budget and the result is verified in a canonical harness-based integration test.
2. **Given** a temporarily unreachable destination, **When** a unicast message is sent, **Then** the runtime buffers it with bounded capacity and priority-based eviction and delivers it once a route becomes available.
3. **Given** a multi-hop routed transfer, **When** chunk acknowledgements reveal gaps or the connection drops mid-transfer, **Then** only the missing chunks are retried, pacing honors observed throughput, and resume starts from the correct acknowledged offset.
4. **Given** an operator requests health or erasure APIs, **When** a health snapshot, `forgetPeer`, or `factoryReset` is invoked, **Then** the runtime reports consistent system state and erases the required persisted/local peer state without violating lifecycle guarantees.
5. **Given** a peer from another app mesh is nearby, **When** advertisements or discovery traffic are processed, **Then** mesh isolation prevents cross-app discovery and connection establishment.

---

### User Story 3 - Release and Compliance Hardening (Priority: P2)

As a maintainer, I need the distribution and verification surface to include the missing security, release-metadata, and cross-platform validation pieces so that the shipped artifacts are reviewable, reproducible, and constitution-compliant.

**Why this priority**: The project already has Maven/XCFramework publication flow, but a few key release responsibilities remain incomplete: weekly CodeQL scanning, finalized SwiftPM binary-target metadata, and explicit evidence that SKIE-backed Swift ergonomics are preserved.

**Independent Test**: CI/release workflow verification plus macOS release-path validation confirm packaging, checksum correctness, API tracking, and release security checks.

**Acceptance Scenarios**:

1. **Given** the repository default branch, **When** scheduled security scanning runs, **Then** a `codeql.yml` workflow scans `actions`, `c-cpp`, and `java-kotlin` on the required cadence.
2. **Given** a release-tag workflow, **When** the XCFramework artifact is packaged, **Then** `Package.swift` references the exact GitHub release asset URL and checksum produced by the workflow rather than a placeholder.
3. **Given** SKIE is enabled for iOS packaging, **When** the Apple framework is produced, **Then** the verification path confirms exhaustive enum bridging and `AsyncSequence` interop for at least one public stream API.

---

### Edge Cases

- What happens when the iOS crypto delegate has not been installed before runtime creation? The remediation must define a deterministic failure mode instead of silently falling back to an unusable state.
- How does the system behave when route convergence time exceeds the 3-second budget in CI? The verification path must fail with explicit evidence rather than rely on manual interpretation.
- What is the behavior when `forgetPeer` or `factoryReset` is invoked while transfers, routes, or sessions are still active? The lifecycle and erasure semantics must be explicit and testable.
- What happens when L2CAP capability is inconsistent across reconnects or OEM cache entries go stale? Fallback behavior must remain bounded and observable.
- How does store-and-forward eviction behave when the buffer is full and a higher-priority message arrives? The eviction rule must be deterministic and test-covered.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Android and iOS transport actuals MUST replace `VirtualMeshTransport` delegation with real BLE implementations while preserving the shared `BleTransport` contract.
- **FR-002**: Noise XX handshake flow MUST perform real DH, hashing, key derivation, and TrustStore consultation so accepted peers establish real transport encryption sessions.
- **FR-003**: MeshEngine MUST wire routing, transfer, power, and encrypted delivery behavior into the runtime path rather than stopping at transport + diagnostic scaffolding.
- **FR-004**: Babel routing remediation MUST add canonical `MeshTestHarness`-based multi-node verification and explicit convergence-budget evidence for 10-node topology changes.
- **FR-005**: Messaging and transfer remediation MUST complete cut-through relay semantics, bounded store-and-forward buffering, retransmit-limit enforcement, selective-ack handling, and throughput-based pacing feedback.
- **FR-006**: Public API remediation MUST expose implementation-backed operational capabilities still missing from the codebase, including health snapshots and GDPR-aligned per-peer/full reset behavior where required by the umbrella spec.
- **FR-007**: Discovery and advertisement handling MUST enforce mesh isolation using the configured application identifier without breaking existing shared API contracts or backward compatibility expectations.
- **FR-008**: Platform-distribution remediation MUST add missing release/security verification pieces, including CodeQL workflow coverage, non-placeholder SwiftPM checksum validation, and explicit SKIE output verification.
- **FR-009**: The remediation feature MUST preserve BCV/API-tracking expectations and update baselines only when public API changes are intentional and documented.
- **FR-010**: The remediation feature MUST remain constitution-compliant: no new server dependency, no new runtime dependency beyond coroutines, no external crypto runtime in shipped artifacts, and no wire-breaking change without an explicit compatibility strategy.

### Key Entities *(include if feature involves data/state)*

- **MeshSessionRegistry**: Runtime-owned mapping of peers to transport session, trust outcome, route reachability, and transfer state needed to close the current orchestration gap.
- **StoreForwardBufferEntry**: Buffered outbound message/transfer metadata used for temporarily unreachable destinations with deterministic eviction and retry semantics.
- **MeshHealthSnapshot**: Public-facing operational snapshot containing connected peers, routing table size, active transfers, buffer usage, and current power mode.
- **ErasureScope**: Internal representation of per-peer forget vs full reset semantics, including which local state stores and runtime registries must be cleared.

## Wire Protocol Impact *(include if feature adds/modifies messages)*

- **New message type(s)**: No new `MessageType` values are planned by default for this remediation wave.
- **Modified messages**: Existing routed, broadcast, and transfer-related payload handling may need internal behavioral changes to satisfy the messaging and transfer specs, but no wire-shape change is planned unless implementation work proves it necessary.
- **Backward compatibility**: This remediation wave should prefer wire-compatible fixes. Any required wire-shape changes must be isolated, documented, and paired with explicit backward-compatibility handling before implementation proceeds.
- **Wire codec changes**: Potential touchpoints include `wire/WireCodec.kt`, `wire/InboundValidator.kt`, and selected files under `wire/messages/` if transfer or relay behavior requires message-level clarification.
- **FlatBuffers schema**: No FlatBuffers schema change is planned in this remediation wave.

> If implementation proves a wire change is unavoidable, update this section before tasks are generated.

## Platform Actuals *(include if feature requires platform-specific code)*

| Platform | Source Set | Implementation Notes |
|----------|-----------|---------------------|
| Common | `commonMain` | Runtime orchestration, routing/messaging/transfer/power integration, health snapshot and erasure behavior, shared crypto/trust contracts |
| Android | `androidMain` | Replace virtual transport façade with real Android BLE implementation and keep public API surface aligned with common contracts |
| iOS | `iosMain` | Replace virtual transport façade with real CoreBluetooth/L2CAP behavior and preserve the Swift-installed crypto delegate bridge |
| JVM | `jvmMain` | Maintain benchmark and crypto verification support; no production runtime expansion beyond test/benchmark infrastructure |

## Security Analysis *(mandatory for crypto/trust/transport features)*

- **Threat model**: Passive BLE observers, unauthenticated peers, route manipulation during convergence, replay/tampering during transport, stale-device fallback behavior, and release-pipeline supply-chain drift.
- **Crypto primitives used**: Existing `CryptoProvider` primitives — X25519, Ed25519, ChaCha20-Poly1305, HKDF-SHA256, HMAC-SHA256.
- **Key material handling**: Trust and session establishment remain behind `CryptoProvider` + `TrustStore`; remediation work must keep iOS real cryptography inside the existing Swift delegate bridge and avoid shipping third-party crypto binaries.
- **Wycheproof coverage**: Continue using the existing Wycheproof vector suite for ChaCha20-Poly1305, Ed25519, X25519, and HKDF; add any missing coverage needed to support newly wired runtime paths.

## Performance Budget *(mandatory)*

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Routing convergence | `< 3s` for 10-node topology change | Canonical harness integration test + supporting benchmark evidence |
| Real transport secure handshake | Maintain existing handshake budget intent without regressing published targets | Device-path integration tests + benchmark spot checks |
| Transfer retransmission overhead | Only missing chunks retried; no unnecessary full-transfer restart | Transfer integration tests + `TransferBenchmark` regression check |
| Release verification | No >10% regression in existing benchmark-covered operations introduced by remediation | `RoutingBenchmark`, `DedupBenchmark`, `TransferBenchmark`, `WireFormatBenchmark` |

## Public API Surface *(include if feature modifies MeshLinkApi)*

- **New public types**: Likely `MeshHealthSnapshot` (or equivalent) if health reporting is surfaced through the public API.
- **Modified signatures**: Likely additions around operational APIs such as health snapshot retrieval, per-peer forget, and full reset if these are exposed through `MeshLinkApi`.
- **Deprecations**: None planned initially; prefer additive changes with BCV-reviewed baselines.
- **SKIE impact**: Any new public streams or state snapshots must remain friendly to SKIE-generated Swift wrappers and preserve exhaustive enum / `AsyncSequence` expectations.

## Diagnostic Events *(include if feature emits diagnostics)*

- **New codes**: No new diagnostic codes are planned by default; the remediation wave should reuse the existing 26-code catalog where possible.
- **Payload**: Existing diagnostic payloads may need broader usage by newly wired runtime paths (handshake failures, route changes, buffer pressure, transfer failures, identity rotation).
- **Existing code reuse**: Prefer `HANDSHAKE_*`, `ROUTE_*`, `MESSAGE_*`, `TRANSFER_*`, `POWER_TIER_CHANGED`, `IDENTITY_ROTATED`, and `BUFFER_PRESSURE` before proposing catalog expansion.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Android and iOS transport actuals no longer delegate to `VirtualMeshTransport` in production code paths.
- **SC-002**: Canonical harness-based integration tests cover multi-hop routing convergence, cut-through relay behavior, and at least one resumed transfer scenario.
- **SC-003**: The 10-node routing convergence budget is explicitly verified with code evidence and passes within the required threshold.
- **SC-004**: Release/security hardening closes the remaining platform-distribution gaps: CodeQL workflow exists, release packaging validates `Package.swift`, and SKIE verification evidence is automated or reproducible in CI/release docs.
- **SC-005**: Shared remediation changes maintain the project’s 100% line and branch coverage gate and do not regress benchmark-covered operations by more than 10%.

## Assumptions

- The umbrella remediation feature is allowed to coordinate changes across multiple subsystem packages because the user explicitly requested a cross-spec remediation wave.
- Existing subsystem specs remain canonical; this umbrella feature does not replace them, but closes the implementation gaps identified across them.
- Wire compatibility should be preserved unless remediation work proves a specific protocol change is unavoidable.
- Real Android/iOS transport work may require host/device-specific verification beyond what can run on all local environments; tasks should separate shared logic verification from device-path validation.

## Subsystem Placement

- **Primary package**: Cross-subsystem umbrella touching `api`, `crypto`, `crypto/noise`, `engine`, `messaging`, `power`, `routing`, `transfer`, `transport`, `wire`, and workflow/build infrastructure.
- **Touches**: `.github/workflows`, `Package.swift`, `meshlink/build.gradle.kts`, BCV baselines, platform actuals, shared integration tests, and benchmark sources.
- **New package**: Prefer shared test-support extraction (for canonical harness reuse) over introducing a new production package.
