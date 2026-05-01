# Tasks: Platform & Distribution

**Input**: Design documents from `specs/platform-distribution/`, including active feature memory  
**Prerequisites**: `spec.md`, `plan.md`, `memory-synthesis.md`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared state)
- **[Story]**: `US0` foundation, `US1` Android distribution, `US2` iOS device distribution, `US3` binary compatibility, `US4` CI quality gates
- Every task includes exact file paths.
- iOS verification is **device-only**; do not create simulator-based acceptance tasks.
- Any dependency or plugin version change must be made in `gradle/libs.versions.toml` using `version.ref`.

## Verification Commands

```bash
./gradlew :meshlink:ktfmtCheck :meshlink:detekt :meshlink:jvmTest :meshlink:androidHostTest :meshlink:koverVerify :meshlink:apiCheck
./gradlew :meshlink:jvmCiBenchmark
./gradlew :meshlink:compileKotlinIosArm64 :meshlink:assembleMeshLinkReleaseXCFramework
./scripts/verify-publish.sh meshlink/build/outputs/aar meshlink/build/XCFrameworks/release
swift package compute-checksum meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip
```

---

## Phase 1: Foundation (US0)

**Purpose**: Shared build, packaging, and verification infrastructure required by all stories.

- [x] T001 [US0] Normalize dependency and plugin version declarations to `version.ref` entries in `gradle/libs.versions.toml`.
- [x] T002 [US0] Update root build configuration in `build.gradle.kts` to consume version-catalog aliases and enforce BCV/KLib validation.
- [x] T003 [US0] Update module build configuration in `meshlink/build.gradle.kts` for publishing, signing, Dokka, XCFramework, benchmark, and SKIE wiring.
- [x] T004 [US0] Create XCFramework packaging helper in `scripts/package-xcframework.sh`.
- [x] T005 [US0] Create SwiftPM manifest update helper in `scripts/update-package-swift.sh`.
- [x] T006 [US0] Refresh publication verification in `scripts/verify-publish.sh` so release archives are scanned for forbidden crypto payloads.
- [x] T007 [US0] Verify foundation wiring with `./gradlew :meshlink:tasks --all` and confirm `assembleMeshLinkReleaseXCFramework`, `publishAllPublicationsToOSSRHRepository`, `apiCheck`, and `jvmCiBenchmark` are available.

**Gate**: Foundation tasks complete before story work begins.

---

## Phase 2: User Story 1 — Android Distribution (Priority: P1) 🎯 MVP

**Goal**: Publish Android/JVM artifacts with constitution-compliant platform actuals and Android-specific packaging support.  
**Verification**: `./gradlew :meshlink:jvmTest :meshlink:androidHostTest :meshlink:koverVerify`

### Implementation

- [ ] T008 [US1] Implement Android crypto actual in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/crypto/AndroidCryptoProvider.kt`.
<!-- parallel-group: 1 -->
- [ ] T009 [P] [US1] Implement Android secure storage in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/storage/AndroidSecureStorage.kt`.
- [ ] T010 [P] [US1] Implement the Android background BLE service in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/MeshLinkService.kt`.
- [ ] T011 [US1] Implement Android BLE transport in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/transport/AndroidBleTransport.kt`.
- [ ] T012 [US1] Wire Android platform dependencies in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/api/MeshLinkAndroidFactory.kt` after T008-T011.
<!-- parallel-group: 2 -->
- [ ] T013 [P] [US1] Add Android crypto provider coverage in `meshlink/src/androidHostTest/kotlin/ch/trancee/meshlink/crypto/AndroidCryptoProviderTest.kt`.
- [ ] T014 [P] [US1] Add Android factory/distribution coverage in `meshlink/src/androidHostTest/kotlin/ch/trancee/meshlink/api/MeshLinkAndroidFactoryTest.kt`.
- [ ] T015 [US1] Refresh Android consumer rules in `consumer-rules.pro`.
- [ ] T016 [US1] Verify Android distribution with `./gradlew :meshlink:jvmTest :meshlink:androidHostTest :meshlink:koverVerify`.

**Checkpoint**: Android artifacts are publishable and independently verifiable.

---

## Phase 3: User Story 2 — iOS Device Distribution (Priority: P1)

**Goal**: Ship a physical-device-only XCFramework and SwiftPM binary-target manifest for iOS BLE consumers.  
**Verification**: `./gradlew :meshlink:compileKotlinIosArm64 :meshlink:assembleMeshLinkReleaseXCFramework`

### Implementation

- [ ] T017 [US2] Implement the iOS crypto bridge contract in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoBridge.kt`.
- [ ] T018 [US2] Implement the iOS crypto provider in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoProvider.kt`.
<!-- parallel-group: 3 -->
- [ ] T019 [P] [US2] Implement iOS secure storage in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/storage/IosSecureStorage.kt`.
- [ ] T020 [P] [US2] Implement the iOS helper node in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/engine/MeshNode.kt`.
- [ ] T021 [US2] Implement physical-device BLE transport in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/transport/IosBleTransport.kt`.
- [ ] T022 [US2] Wire iOS platform dependencies in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/api/MeshLinkIosFactory.kt` after T017-T021.
- [x] T023 [US2] Create the release-hosted SwiftPM binary-target manifest in `Package.swift`.
- [x] T024 [US2] Document physical-device-only iOS integration and unsupported simulator execution in `docs/ios-crypto-bridge.md`.
- [x] T025 [US2] Verify device-only iOS packaging with `./gradlew :meshlink:compileKotlinIosArm64 :meshlink:assembleMeshLinkReleaseXCFramework`.

**Checkpoint**: iOS XCFramework distribution works for physical-device workflows and states simulator limits explicitly.

---

## Phase 4: User Story 3 — Binary Compatibility & Swift Interop (Priority: P1)

**Goal**: Track JVM and iOS API baselines and verify the Swift-facing surface stays stable.  
**Verification**: `./gradlew :meshlink:apiCheck`

### Implementation

- [ ] T026 [US3] Refresh the JVM BCV baseline in `meshlink/api/jvm/meshlink.api` via `./gradlew :meshlink:apiDump`.
- [ ] T027 [US3] Refresh the iOS KLib baseline in `meshlink/api/meshlink.klib.api` via `./gradlew :meshlink:apiDump` on macOS.
- [ ] T028 [US3] Configure or refine SKIE in `meshlink/build.gradle.kts`.
- [ ] T029 [US3] Add Swift interop verification guidance for `MeshLinkState` and one public stream API in `docs/ios-crypto-bridge.md`.
- [ ] T030 [US3] Verify API compatibility with `./gradlew :meshlink:apiCheck`.

**Checkpoint**: Public API baselines are committed and Swift-facing behavior is explicitly verified.

---

## Phase 5: User Story 4 — CI Quality Gates (Priority: P1)

**Goal**: Make PR, release, and security workflows enforce the full distribution contract.  
**Verification**: full verification commands plus release-asset checksum validation.

### Implementation

- [x] T031 [US4] Create or refresh PR validation in `.github/workflows/ci.yml` for `ktfmt`, `detekt`, `jvmTest`, `androidHostTest`, `koverVerify`, `apiCheck`, and `jvmCiBenchmark`.
<!-- parallel-group: 4 -->
- [x] T032 [P] [US4] Create or refresh release automation in `.github/workflows/release.yml` for `publish-android`, `publish-ios`, XCFramework zip packaging, checksum generation, `Package.swift` validation, and GitHub release asset upload.
- [ ] T033 [P] [US4] Create or refresh scheduled scanning in `.github/workflows/codeql.yml`.
- [x] T034 [P] [US4] Create or refresh local quality hooks in `.githooks/pre-commit`.
- [x] T035 [US4] Verify release artifacts with `./scripts/verify-publish.sh meshlink/build/outputs/aar meshlink/build/XCFrameworks/release`.
- [x] T036 [US4] Verify SwiftPM checksum generation with `swift package compute-checksum meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip`.
- [ ] T037 [US4] Run the final gate: `./gradlew :meshlink:ktfmtCheck :meshlink:detekt :meshlink:jvmTest :meshlink:androidHostTest :meshlink:koverVerify :meshlink:apiCheck && ./gradlew :meshlink:jvmCiBenchmark`.

**Checkpoint**: PR, release, and security gates enforce the full platform-distribution contract.

---

## Dependencies & Execution Order

- Phase 1 blocks all other work.
- Phase 2 depends on Phase 1.
- Phase 3 depends on Phase 1.
- Phase 4 depends on Phases 2 and 3 because BCV and SKIE verification must run against the final public surface.
- Phase 5 depends on Phases 2-4 because CI and release automation must validate the finished Android/iOS/build pipeline.

## Parallel Opportunities

- `T009` and `T010` can run together after `T008`.
- `T013` and `T014` can run together after `T012`.
- `T019` and `T020` can run together after `T018`.
- `T032`, `T033`, and `T034` can run together after `T031`.

## Coverage Strategy

Per constitution: 100% line + branch coverage, no `@CoverageIgnore`.

- Add or refresh Android host tests for Android-only platform logic.
- Keep shared/public-surface verification under `jvmTest` and `apiCheck`.
- Use explicit release-asset verification to cover crypto packaging constraints.
- Treat device-only iOS integration guidance as part of the acceptance surface; do not add simulator-based tests.
