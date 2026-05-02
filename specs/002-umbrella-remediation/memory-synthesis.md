# Memory Synthesis

feature: 002-umbrella-remediation
status: planning
hard_conflicts: 0
soft_conflicts: 0
assumptions_to_confirm: 2

## Current Constraints
- [C1] Kotlin must remain pinned to 2.3.20 until SKIE compatibility is re-verified.
- [C2] No remediation step may add a new runtime dependency beyond `kotlinx-coroutines-core`.
- [C3] Platform BLE actuals must stay behind the shared `BleTransport` contract.
- [C4] Canonical integration tests must use `MeshTestHarness` + `VirtualMeshTransport` rather than new ad hoc harnesses.

## Reused Decisions
- [D1] Keep iOS cryptography behind the existing Swift-installed delegate bridge (`IosCryptoProvider` / `MeshLinkIosFactory`).
- [D2] Keep shared logic in `commonMain` and platform-specific glue in `androidMain` / `iosMain` only.
- [D3] Preserve BCV-tracked API surfaces and update baselines only for intentional public API changes.

## Relevant Bug Patterns
- [none]

## Architecture Boundaries
- [B1] Treat subsystem specs as canonical and use this umbrella feature only to coordinate gap closure across them.
- [B2] Prefer behavior-only remediation over wire-shape changes; wire changes require explicit compatibility planning.
- [B3] Expand `MeshEngine` as the runtime orchestrator rather than introducing a DI framework or new orchestration layer.

## Feature-to-Memory Conflicts
- [none]

## Assumptions Requiring Confirmation
- [A1] Health snapshot and erasure APIs belong in this remediation wave rather than a separate public-API feature.
- [A2] Application-ID mesh isolation can be completed without introducing a new advertisement wire contract.

## Implementation Watchpoints
- [W1] Replacing `VirtualMeshTransport` delegates in Android/iOS code will likely cascade into service/factory/test updates.
- [W2] Real Noise XX integration must hook TrustStore decisions into handshake acceptance, not just validate message order.
- [W3] Transfer remediation must use `retransmitLimit`, consume SACK information, and feed pacing decisions back into send behavior.
- [W4] Routing remediation must add explicit 10-node / <3s convergence proof and clean up any constitution-invalid validation style.
- [W5] Release hardening must include `codeql.yml` and non-placeholder `Package.swift` checksum validation.

## Verification Watchpoints
- [V1] Re-run `jvmTest`, `androidHostTest`, `koverVerify`, and benchmark suites after remediation tasks land.
- [V2] Validate iOS packaging on macOS because SKIE/XCFramework verification cannot be trusted from non-Apple build paths alone.
- [V3] Any public API addition must be reflected in both JVM and KLib BCV baselines with explicit rationale.
