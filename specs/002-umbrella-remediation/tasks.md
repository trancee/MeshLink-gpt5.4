# Tasks: Umbrella Remediation Wave 1

**Input**: Design documents from `specs/002-umbrella-remediation/`, including active feature memory  
**Prerequisites**: `specs/002-umbrella-remediation/plan.md`, `specs/002-umbrella-remediation/spec.md`, `specs/002-umbrella-remediation/memory-synthesis.md`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared state)
- **[Story]**: `Found`, `US1`, `US2`, or `US3`
- Include exact file paths in every task
- Any human or automated commit created while executing these tasks MUST use a Conventional Commit message
- Preserve the watchpoints from `memory-synthesis.md` during implementation and verification

## Verification Commands

```bash
# Full verification (must pass before merge)
./gradlew :meshlink:jvmTest :meshlink:androidHostTest :meshlink:koverVerify :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck

# Shared/runtime checks
./gradlew :meshlink:jvmTest
./gradlew :meshlink:koverVerify
./gradlew :meshlink:koverHtmlReport
./gradlew :meshlink:apiCheck
./gradlew :meshlink:detekt
./gradlew :meshlink:ktfmtCheck
./gradlew :meshlink:ktfmtFormat

# Android/iOS platform checks
./gradlew :meshlink:androidHostTest
./gradlew :meshlink:compileKotlinIosArm64

# Benchmarks
./gradlew :meshlink:jvmBenchmark
./gradlew :meshlink:jvmCiBenchmark
```

## Watchpoints Carried Into Tasking

### Implementation Watchpoints

- **[W1]** Replacing `VirtualMeshTransport` delegates in Android/iOS code will cascade into service/factory/test updates.
- **[W2]** Real Noise XX integration must hook `TrustStore` decisions into handshake acceptance, not just validate message order.
- **[W3]** Transfer remediation must use `retransmitLimit`, consume SACK information, and feed pacing decisions back into send behavior.
- **[W4]** Routing remediation must add explicit 10-node / `<3s` convergence proof and clean up constitution-invalid validation style.
- **[W5]** Release hardening must include `.github/workflows/codeql.yml` and non-placeholder `Package.swift` checksum validation.

### Verification Watchpoints

- **[V1]** Re-run `jvmTest`, `androidHostTest`, `koverVerify`, and benchmark suites after remediation tasks land.
- **[V2]** Validate iOS packaging on macOS because SKIE/XCFramework verification cannot be trusted from non-Apple build paths alone.
- **[V3]** Any public API addition must be reflected in both `meshlink/api/jvm/meshlink.api` and `meshlink/api/meshlink.klib.api` with explicit rationale.

---

## Phase 1: Foundation (Shared Infrastructure)

**Purpose**: Extract shared test infrastructure and define the core runtime types that multiple remediation slices depend on.  
**No user story work should start until this phase is complete.**

- [x] T001 [Found] Extract the canonical harness from `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/MeshEngineIntegrationTest.kt` into `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/harness/MeshTestHarness.kt` for reuse by routing, messaging, and transfer integration tests.
- [x] T002 [P] [Found] Create `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshSessionRegistry.kt` to centralize peer session, trust, route, and transfer coordination state referenced by `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt`.
- [x] T003 [P] [Found] Create `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/ErasureScope.kt` to represent per-peer forget vs full reset semantics used by `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt`.
- [x] T004 [P] [Found] Create `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshHealthSnapshot.kt` for the public operational snapshot surfaced by `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshLinkApi.kt`.
- [x] T005 [P] [Found] Add foundation tests in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/harness/MeshTestHarnessTest.kt`, `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/MeshSessionRegistryTest.kt`, and `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/api/MeshHealthSnapshotTest.kt`.
- [x] T006 [Found] Verify the foundation phase with `./gradlew :meshlink:jvmTest :meshlink:detekt :meshlink:ktfmtCheck`.

**Gate**: Foundation types and canonical harness are stable enough for downstream story work.

---

## Phase 2: User Story 1 — Real Device Transport and Secure Session Establishment (Priority: P1) 🎯 MVP

**Goal**: Replace platform BLE stubs with real transport behavior and turn Noise XX from round tracking into real trust-gated session establishment.  
**Verification**: Android host tests, iOS compilation, and shared Noise/handshake tests prove secure session establishment and fallback behavior.

### Implementation

- [x] T007 [US1] Implement real XX key schedule and cipher/session derivation in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/noise/NoiseXXHandshake.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/noise/CipherState.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/noise/SymmetricState.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/noise/NoiseSession.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/noise/NoiseKSeal.kt`, and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/noise/NoiseKOpen.kt`.
- [x] T008 [P] [US1] Expand handshake/session coverage in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/crypto/noise/NoiseXXHandshakeTest.kt` and create `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/crypto/noise/NoiseSessionTest.kt` for matching-session derivation, failure cases, and transcript integrity.
- [x] T009 [US1] Integrate trust-gated acceptance into `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/NoiseHandshakeManager.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/crypto/TrustStore.kt`, and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt` to satisfy watchpoint [W2].
- [x] T010 [P] [US1] Add handshake-manager integration coverage in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/NoiseHandshakeManagerDiagnosticTest.kt` and create `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/NoiseHandshakeManagerIntegrationTest.kt` for accepted, rejected, and prompt-driven trust paths.
- [x] T011 [US1] Replace the Android virtual delegate path in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/AndroidBleTransport.kt` with real BLE lifecycle logic while preserving the `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transport/BleTransport.kt` contract.
- [x] T012 [P] [US1] Update the Android service façade in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/MeshLinkService.kt` and validate it in `meshlink/src/androidHostTest/kotlin/ch/trancee/meshlink/transport/MeshLinkServiceTest.kt` to cover the cascading changes from watchpoint [W1].
- [x] T013 [P] [US1] Update Android transport host tests in `meshlink/src/androidHostTest/kotlin/ch/trancee/meshlink/transport/AndroidBleTransportTest.kt` for discovery, connection, send, and fallback semantics without `VirtualMeshTransport` delegation.
- [x] T014 [US1] Replace the iOS virtual delegate path in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/transport/IosBleTransport.kt` and wire any required bootstrap changes through `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/api/MeshLinkIosFactory.kt` while preserving the existing Swift-installed crypto delegate bridge.
- [x] T015 [P] [US1] Update iOS runtime bootstrap glue in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/engine/MeshNode.kt` and `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoBridge.kt` if needed so the real transport path and handshake lifecycle compile together.
- [x] T016 [US1] Implement explicit L2CAP-to-GATT fallback and stale-capability handling in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transport/ConnectionInitiationPolicy.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transport/OemL2capProbeCache.kt`, `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/AndroidBleTransport.kt`, and `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/transport/IosBleTransport.kt`.
- [x] T017 [P] [US1] Extend shared transport tests in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/transport/ConnectionInitiationPolicyTest.kt` and `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/transport/OemL2capProbeCacheTest.kt` for stale OEM-cache and fallback edge cases.
- [x] T018 [US1] Verify User Story 1 with `./gradlew :meshlink:jvmTest :meshlink:androidHostTest :meshlink:compileKotlinIosArm64 :meshlink:koverVerify`.

**Checkpoint**: Secure sessions are derived through real Noise XX + TrustStore flow, and platform transports no longer depend on production-time `VirtualMeshTransport` delegation.

---

## Phase 3: User Story 2 — Runtime Gap Closure Across Routing, Messaging, Transfers, and Operations (Priority: P1)

**Goal**: Close the shared-runtime gaps across routing, encrypted delivery, buffering, transfers, mesh isolation, and operational APIs.  
**Verification**: Canonical `MeshTestHarness` integration tests prove convergence, cut-through relay, resume/retry behavior, health snapshots, erasure semantics, and app-mesh isolation.

### Implementation

- [x] T019 [US2] Fix routing validation/orchestration in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/routing/RouteCoordinator.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/routing/RoutingEngine.kt`, and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt`, including the constitution-invalid interpolated `require()` pattern referenced by watchpoint [W4].
- [ ] T020 [P] [US2] Add canonical routing integration tests in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/routing/RoutingHarnessIntegrationTest.kt` and `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/routing/RoutingConvergenceIntegrationTest.kt` with an explicit 10-node / `<3s` convergence assertion.
- [ ] T021 [P] [US2] Extend routing coverage in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/routing/RoutingFoundationTest.kt`, `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/routing/RoutingEngineTest.kt`, and `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/routing/RouteCoordinatorSeqNoTest.kt` so convergence and validation paths stay at 100% coverage.
- [ ] T022 [US2] Complete cut-through relay mutation and bounded store-and-forward buffering in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/messaging/CutThroughBuffer.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/messaging/DeliveryPipeline.kt`, and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/messaging/MessagingConfig.kt`.
- [ ] T023 [P] [US2] Add messaging coverage in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/messaging/CutThroughRelayIntegrationTest.kt`, create `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/messaging/StoreForwardBufferIntegrationTest.kt`, and extend `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/messaging/DeliveryPipelineTest.kt` for deterministic eviction and delayed-route delivery.
- [ ] T024 [US2] Enforce retransmit limits, selective-ack consumption, and pacing feedback in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/TransferEngine.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/TransferSession.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/SackTracker.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/ObservationRateController.kt`, and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/ResumeCalculator.kt` to satisfy watchpoint [W3].
- [ ] T025 [P] [US2] Extend transfer coverage in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/transfer/TransferEngineTest.kt`, `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/transfer/TransferSessionTest.kt`, and create `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/transfer/TransferResumeIntegrationTest.kt` for gap-only retries and correct resume offsets.
- [ ] T026 [US2] Wire routing, transfer, power, buffered delivery, and session registry coordination through `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt` and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshStateManager.kt`.
- [ ] T027 [P] [US2] Extend engine-level end-to-end coverage in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/MeshEngineIntegrationTest.kt`, `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/MeshEngineRoutingTest.kt`, and `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/MeshEngineDeliveryPipelineTest.kt` for routed encrypted send paths and transfer orchestration.
- [ ] T028 [US2] Expose operational APIs in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshLinkApi.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshLink.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshHealthSnapshot.kt`, and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt` for health snapshots, `forgetPeer`, and `factoryReset`.
- [ ] T029 [P] [US2] Implement persistent-state erasure on platform storage backends in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/storage/AndroidSecureStorage.kt` and `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/storage/IosSecureStorage.kt`, then cover API behavior in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/MeshEngineApiLifecycleTest.kt` and create `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/MeshEngineErasureIntegrationTest.kt`.
- [ ] T030 [US2] Enforce application-ID mesh isolation in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transport/AdvertisementCodec.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transport/MeshHashFilter.kt`, `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/AndroidBleTransport.kt`, and `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/transport/IosBleTransport.kt`.
- [ ] T031 [P] [US2] Add mesh-isolation tests in `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/transport/AdvertisementCodecTest.kt`, `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/transport/MeshHashFilterTest.kt`, and create `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/engine/PeerIsolationIntegrationTest.kt`.
- [ ] T032 [US2] Verify User Story 2 with `./gradlew :meshlink:jvmTest :meshlink:koverVerify` and inspect `./gradlew :meshlink:koverHtmlReport` for any new uncovered branches before proceeding.

**Checkpoint**: The shared runtime matches the umbrella spec for routing convergence, buffered/cut-through delivery, resumed transfers, operational APIs, and mesh isolation.

---

## Phase 4: User Story 3 — Release and Compliance Hardening (Priority: P2)

**Goal**: Close the remaining workflow, release metadata, and cross-platform packaging verification gaps.  
**Verification**: Workflow definitions, release packaging, and documented macOS validation paths cover CodeQL, SwiftPM checksum generation, and SKIE verification evidence.

### Implementation

- [ ] T033 [US3] Create `.github/workflows/codeql.yml` with scheduled and PR coverage for `actions`, `c-cpp`, and `java-kotlin` to satisfy watchpoint [W5].
- [ ] T034 [P] [US3] Update `.github/workflows/ci.yml` so the remediation feature’s required checks (`:meshlink:apiCheck`, `:meshlink:koverVerify`, `:meshlink:detekt`, `:meshlink:ktfmtCheck`, and any chosen benchmark smoke path) are enforced consistently.
- [ ] T035 [US3] Update `.github/workflows/release.yml` to compute the XCFramework checksum, publish the asset metadata consumed by SwiftPM, and validate the generated package reference before release completion.
- [ ] T036 [P] [US3] Replace the placeholder binary-target checksum flow in `Package.swift` with the release-generated artifact URL/checksum contract expected by `.github/workflows/release.yml`.
- [ ] T037 [US3] Update `meshlink/build.gradle.kts` if needed so release verification, BCV checks, and benchmark entry points stay aligned with the hardened CI/release workflows.
- [ ] T038 [P] [US3] Add maintainer documentation in `docs/platform-distribution-remediation.md` covering CodeQL cadence, checksum update flow, macOS-only release verification, and the SKIE validation expectations from watchpoint [V2].
- [ ] T039 [US3] Verify User Story 3 by dry-running the repository verification commands and confirming the workflow/package files are internally consistent: `./gradlew :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck`.

**Checkpoint**: The repo has a complete security/release hardening story, and the remaining platform-distribution gaps identified in the umbrella analysis are closed.

---

## Phase 5: Public API & Documentation

**Purpose**: Make intentional public API changes explicit, BCV-tracked, and documented.

- [ ] T040 [US2] Update `meshlink/api/jvm/meshlink.api` and `meshlink/api/meshlink.klib.api` after intentional public API changes from `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshLinkApi.kt` and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshHealthSnapshot.kt`, satisfying watchpoint [V3].
- [ ] T041 [P] [US2] Add or refine KDoc in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshLinkApi.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/MeshHealthSnapshot.kt`, and any new public enums/data classes introduced by this remediation wave.
- [ ] T042 [P] [US2] Update any API-facing behavior notes in `docs/platform-distribution-remediation.md` and, if needed, `specs/002-umbrella-remediation/plan.md` when public operational semantics (health snapshots or erasure behavior) are finalized.
- [ ] T043 [US2] Verify API/doc stability with `./gradlew :meshlink:apiCheck :meshlink:compileKotlinIosArm64`.

---

## Phase 6: Performance & Final Verification

**Purpose**: Confirm the remediation wave stays within performance budgets and closes with evidence, not assumptions.

- [ ] T044 [P] [US2] Update benchmark coverage in `meshlink/src/jvmBenchmark/kotlin/ch/trancee/meshlink/routing/RoutingBenchmark.kt`, `meshlink/src/jvmBenchmark/kotlin/ch/trancee/meshlink/routing/DedupBenchmark.kt`, `meshlink/src/jvmBenchmark/kotlin/ch/trancee/meshlink/transfer/TransferBenchmark.kt`, and `meshlink/src/jvmBenchmark/kotlin/ch/trancee/meshlink/wire/WireFormatBenchmark.kt` where remediation changes affect hot paths.
- [ ] T045 [P] [US2] Ensure newly wired runtime paths reuse existing diagnostic codes before adding any new catalog entries by updating emitters in `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/MeshEngine.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/NoiseHandshakeManager.kt`, `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/messaging/DeliveryPipeline.kt`, and `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/TransferEngine.kt`.
- [ ] T046 [US2] Run the full verification gate from watchpoint [V1]: `./gradlew :meshlink:jvmTest :meshlink:androidHostTest :meshlink:koverVerify :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck`.
- [ ] T047 [P] [US2] Run performance and packaging spot checks from watchpoints [V1] and [V2]: `./gradlew :meshlink:jvmCiBenchmark` locally and `./gradlew :meshlink:compileKotlinIosArm64` on macOS, then record any regressions against the plan budgets.

**Final Gate**: All required verification commands pass, benchmark regressions stay within budget, and platform packaging evidence is captured.

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 (Foundation)
  ├──► Phase 2 (US1: real transport + secure sessions)
  ├──► Phase 3 (US2: runtime gap closure)
  └──► Phase 4 (US3: release/compliance hardening)

Phase 2 (US1) ──► Phase 3 (US2)
Phase 3 (US2) ──► Phase 5 (Public API & Docs)
Phase 4 (US3) ──► Phase 5 (Public API & Docs) when docs/release semantics overlap
Phase 5 (Public API & Docs) ──► Phase 6 (Performance & Final Verification)
```

### Recommended Execution Order

1. Complete **Phase 1** first so every subsystem uses the same `MeshTestHarness` and shared runtime types.
2. Do **US1** next because real transport/session establishment is a prerequisite for believable runtime integration.
3. Do **US2** after US1, or split it into thin sub-slices in this order:
   - routing convergence
   - messaging/store-and-forward
   - transfer resume/retry
   - engine orchestration
   - health/erasure APIs
   - mesh isolation
4. Run **US3** in parallel with later US2 slices when workflow-only files are not in conflict.
5. Finish with **Phase 5** and **Phase 6** to lock BCV, docs, benchmarks, and verification evidence.

### Parallel Opportunities

- `T002`–`T005` can run in parallel after `T001` defines the shared harness direction.
- In **US1**, Android tasks (`T011`–`T013`) and iOS tasks (`T014`–`T015`) can proceed in parallel once `T007`–`T010` settle the shared handshake contract.
- In **US2**, routing (`T019`–`T021`), messaging (`T022`–`T023`), and transfer (`T024`–`T025`) can be split across parallel slices before converging in `T026`–`T032`.
- In **US3**, `.github/workflows/codeql.yml` (`T033`) and `Package.swift` / docs updates (`T036`, `T038`) can proceed in parallel if workflow ownership is coordinated.
- Benchmark work (`T044`) and diagnostic-emitter cleanup (`T045`) can run in parallel after functional changes stabilize.

### Coverage Strategy

Per constitution: 100% line + branch coverage, no `@CoverageIgnore` shortcuts.

- Write or update tests in the same slice as each implementation task.
- Reuse `meshlink/src/commonTest/kotlin/ch/trancee/meshlink/harness/MeshTestHarness.kt` for all new multi-node integration tests.
- Use `./gradlew :meshlink:koverHtmlReport` after `T032` and before `T046` to inspect any new branches introduced by coroutine state machines or transport fallback logic.
- If a genuine phantom branch appears, document it in `specs/002-umbrella-remediation/plan.md` before considering any Kover exclusion.
