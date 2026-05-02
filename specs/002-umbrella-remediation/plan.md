# Implementation Plan: Umbrella Remediation Wave 1

**Branch**: `002-umbrella-remediation` | **Date**: 2026-05-01 | **Spec**: `specs/002-umbrella-remediation/spec.md`  
**Input**: Feature specification from `specs/002-umbrella-remediation/spec.md`  
**Memory Inputs**: `docs/memory/DECISIONS.md`, `specs/002-umbrella-remediation/memory.md`, `specs/002-umbrella-remediation/memory-synthesis.md`

## Summary

This remediation wave closes the largest gaps between the migrated MeshLink subsystem specs and the current implementation-bearing repository. The work replaces platform BLE transport stubs with real platform actuals, upgrades the current round-tracking Noise XX scaffolding into a real trust-gated session-establishment path, wires routing/transfer/power into `MeshEngine`, adds missing operational capabilities (buffering, health snapshots, erasure), and hardens platform distribution with the remaining CI/release/security obligations. The plan is intentionally cross-spec because the missing behavior sits at subsystem boundaries, but it preserves the existing package layout, BCV/API-tracking discipline, and wire-compatibility-first posture.

## Technical Context

**Language/Version**: Kotlin 2.3.20 (Kotlin Multiplatform)  
**Targets**: `commonMain`, `androidMain` (API 29+), `iosMain` (arm64, iOS 15+), `jvmMain` (test/benchmark infrastructure)  
**Build System**: Gradle 9.5.0 (Kotlin DSL, version catalog)  
**Primary Dependencies**: `kotlinx-coroutines-core`, platform JCA/CryptoKit bridge through the existing `CryptoProvider` abstraction, existing build/test plugins already pinned in the repo  
**Testing**: `kotlin.test`, Android host tests, canonical `MeshTestHarness` + `VirtualMeshTransport`, Wycheproof vectors, benchmark suites under `jvmBenchmark`  
**Coverage**: Kover — 100% line + branch enforced in `meshlink/build.gradle.kts`  
**Static Analysis**: Detekt + ktfmt  
**API Tracking**: BCV baselines at `meshlink/api/jvm/meshlink.api` and `meshlink/api/meshlink.klib.api`  
**Benchmarks**: `RoutingBenchmark`, `DedupBenchmark`, `TransferBenchmark`, `WireFormatBenchmark`  
**Publishing**: OSSRH / Maven Central + XCFramework + SwiftPM binary target  
**Performance Goals**: Close the outstanding convergence, secure session, transfer retry, and release-verification gaps without regressing existing benchmark-covered operations by >10%  
**Constraints**: No server dependency; no new runtime dependency beyond coroutines; no released third-party crypto binaries; preserve wire compatibility unless a separately justified protocol change becomes unavoidable

## Constitution Check

*GATE: Must pass before implementation. Re-check after design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ | Work stays inside existing package layout, explicit API, BCV-tracked surfaces, and exact-version dependency rules |
| II. Testing | ⚠️ | The remediation specifically exists because canonical harness coverage, convergence proof, and some runtime path tests are still missing; tasks must close those gaps |
| III. UX Consistency | ✅ | Android/iOS behavior is being aligned behind existing common contracts instead of introducing divergent APIs |
| IV. Performance | ⚠️ | Routing convergence, transport handshake, and transfer retry behavior need explicit benchmark/integration evidence before this wave can be considered complete |
| Conventional Commits | ✅ | Feature branch and future commits must use conventional commits |
| No server dependency | ✅ | No internet or server-side component is introduced |
| Wire stability | ✅ | Plan is wire-compatible first; any required message change must be called out before implementation |
| Single runtime dependency | ✅ | No additional runtime dependency is planned |

## Project Structure

### Documentation (this feature)

```text
specs/002-umbrella-remediation/
├── spec.md
├── plan.md
├── tasks.md                    # To be generated after planning
├── memory.md
└── memory-synthesis.md
```

### Source Code Layout

```text
meshlink/src/
├── commonMain/kotlin/ch/trancee/meshlink/
│   ├── api/                    # Public API surface + operational snapshots / reset APIs
│   ├── crypto/                 # TrustStore, identity rotation, provider abstraction
│   │   └── noise/              # Real Noise XX session derivation and transport state
│   ├── engine/                 # Runtime orchestration and subsystem wiring
│   ├── messaging/              # Delivery pipeline, store-and-forward, cut-through relay
│   ├── power/                  # Power tiers, connection policy, graceful drain
│   ├── routing/                # Babel convergence verification and route orchestration
│   ├── transfer/               # SACK retransmission, resume, pacing
│   ├── transport/              # BLE abstraction, advertisements, L2CAP/GATT policies
│   └── wire/                   # Shared codec if remediation needs message-behavior alignment
├── commonTest/kotlin/ch/trancee/meshlink/
│   ├── engine/
│   ├── messaging/
│   ├── routing/
│   ├── transfer/
│   └── harness/                # Canonical multi-node test harness and topology helpers
├── androidMain/kotlin/ch/trancee/meshlink/
│   ├── api/
│   ├── crypto/
│   ├── storage/
│   └── transport/              # Real Android BLE implementation
├── iosMain/kotlin/ch/trancee/meshlink/
│   ├── api/
│   ├── crypto/
│   ├── storage/
│   └── transport/              # Real CoreBluetooth/L2CAP implementation
└── jvmBenchmark/kotlin/ch/trancee/meshlink/
    ├── routing/
    ├── transfer/
    └── wire/
```

### Files This Feature Touches

| Action | File | Reason |
|--------|------|--------|
| Modify | `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/AndroidBleTransport.kt` | Replace virtual delegate stub with real Android BLE behavior |
| Modify | `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/MeshLinkService.kt` | Bind the Android service façade to the real transport lifecycle |
| Modify | `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/transport/IosBleTransport.kt` | Replace virtual delegate stub with real iOS BLE behavior |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/noise/NoiseXXHandshake.kt` | Add real Noise XX session derivation behavior |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/NoiseHandshakeManager.kt` | Integrate TrustStore/session lifecycle into handshake orchestration |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt` | Wire routing, transfer, power, buffering, and operational APIs into the runtime |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/messaging/DeliveryPipeline.kt` | Complete store-and-forward and cut-through relay semantics |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/messaging/CutThroughBuffer.kt` | Replace byte-append stub with structure-aware relay mutation logic |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/TransferEngine.kt` | Enforce retransmit limits and pacing feedback |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/TransferSession.kt` | Consume richer SACK/retry behavior |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/routing/RouteCoordinator.kt` | Constitution-compliant validation and convergence support cleanup |
| Modify | `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshLinkApi.kt` | Add any required health/erase/reset API surface |
| Create | `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/routing/RoutingConvergenceIntegrationTest.kt` | Explicit 10-node / <3s convergence verification |
| Create | `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/routing/RoutingHarnessIntegrationTest.kt` | Canonical harness-based routing propagation/withdrawal validation |
| Create/Modify | `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/harness/MeshTestHarness.kt` | Reuse/extract the canonical multi-node harness for routing/messaging/transfer integration |
| Modify | `.github/workflows/ci.yml` | Add any missing verification steps required by the remediation wave |
| Create | `.github/workflows/codeql.yml` | Close platform-distribution security-scanning gap |
| Modify | `.github/workflows/release.yml` | Finalize checksum/SKIE/release verification flow |
| Modify | `Package.swift` | Remove placeholder checksum and keep release metadata verifiable |
| Modify | `meshlink/build.gradle.kts` | Wire any additional verification tasks and keep BCV/benchmark gates aligned |
| Modify | `meshlink/api/jvm/meshlink.api` | Intentional BCV updates if public API changes |
| Modify | `meshlink/api/meshlink.klib.api` | Intentional KLib baseline updates if public API changes |

## Design Decisions

| Decision | Choice | Rationale | Alternatives Rejected |
|----------|--------|-----------|----------------------|
| Transport remediation | Replace platform stubs behind existing `BleTransport` contract | Preserves API parity while making platform actuals real | Introducing a new platform-specific public transport API would violate UX consistency |
| Handshake integration | Integrate TrustStore and real Noise session derivation at the handshake-manager boundary | Keeps trust, session lifecycle, and diagnostics coordinated in one place | Trust checks inside UI-facing factories or transport classes would fragment security policy |
| Runtime orchestration | Expand `MeshEngine` as the central coordinator instead of adding a DI/container layer | Matches the existing engine spec and constructor-injection model | A new DI framework adds complexity and violates the current architecture direction |
| Integration verification | Reuse/extract the canonical `MeshTestHarness` for multi-node routing/messaging/transfer tests | Satisfies the constitution and keeps cross-subsystem tests consistent | Building ad hoc subsystem-specific harnesses would duplicate infrastructure and drift from project standards |
| Wire compatibility | Prefer behavior fixes first; treat message-shape changes as opt-in exceptions | Most identified gaps are orchestration/verification gaps, not protocol-definition gaps | Broad wire changes would raise compatibility risk across multiple specs |
| Public API additions | Add only the operational APIs required to close explicit umbrella gaps (health snapshot, erasure) | Keeps BCV churn intentional and reviewable | Folding operational state into internal-only helpers would leave product-level gaps unresolved |

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Real platform BLE work expands beyond one remediation wave | High | High | Keep the umbrella plan phased so tasks separate Android, iOS, and shared-code closure |
| Noise/Trust integration forces public API or wire changes | Medium | High | Verify contract boundaries before tasks are written; document any BCV or wire impact explicitly |
| 10-node convergence timing is flaky in CI | Medium | High | Prefer deterministic harness timing where possible and pair integration proof with benchmark evidence |
| Store-and-forward / erase-reset semantics cut across many subsystems | High | Medium | Centralize ownership in `MeshEngine` and define entity/state boundaries before implementation |
| Release hardening spans local + macOS-only verification paths | Medium | Medium | Keep workflow validation tasks explicit and isolate macOS-only checks to the existing packaging lane |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None currently identified | — | — |
