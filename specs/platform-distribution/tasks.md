# Tasks: Platform & Distribution

**Status**: In progress — platform actuals and distribution scaffolding partially implemented on feature branch

## Phase 1: Android Platform

- [x] ~~[REMOVED] T001 Implement `AndroidCryptoProvider` (libsodium JNI bridge via SodiumJni)~~ — removed to align with the constitution ban on shipping external crypto libraries
- [x] ~~[REMOVED] T002 [P] Implement `SodiumJni` (JNI native method declarations)~~ — removed to align with the constitution ban on shipping external crypto libraries
- [x] ~~[REMOVED] T003 [P] Build libsodium for Android (arm64-v8a, armeabi-v7a, x86_64) — `scripts/build-android-libsodium.sh`~~ — removed to align with the constitution ban on shipping external crypto libraries
- [x] ~~[REMOVED] T004 [P] Build JNI bridge — `scripts/build-android-jni.sh`~~ — removed because the JNI bridge was only required for the deprecated external crypto packaging path
- [x] T005 Implement `AndroidBleTransport` (BluetoothGatt, L2CAP, scanning, advertising)
- [x] T006 [P] Implement `MeshLinkService` (foreground service for background BLE)
- [x] T007 [P] Implement `AndroidSecureStorage` (DataStore preferences)
- [x] T008 Implement `MeshLinkAndroidFactory` (wires all Android actuals)

## Phase 2: iOS Platform

- [x] ~~[REMOVED] T009 Build libsodium for iOS (arm64) — `scripts/build-ios-libsodium.sh`~~ — removed to align with the constitution ban on shipping external crypto libraries
- [x] ~~[REMOVED] T010 Create `libsodium.def` cinterop definition~~ — removed to align with the constitution ban on shipping external crypto libraries
- [x] ~~[REMOVED] T011 Implement `IosCryptoProvider` (libsodium cinterop)~~ — removed to align with the constitution ban on shipping external crypto libraries
- [x] T012 Implement `IosBleTransport` (CBCentralManager, CBPeripheralManager, CBL2CAPChannel)
- [x] T013 [P] Implement `IosSecureStorage`
- [x] T014 [P] Implement `MeshNode` (iOS-specific helper)
- [x] T015 Implement `MeshLinkIosFactory`

## Phase 3: JVM Infrastructure

- [x] T016 Implement `JvmCryptoProvider` (JDK shim for test execution)
- [x] T017 [P] Add `DedupBenchmark`, `RoutingBenchmark`, `TransferBenchmark`, `WireFormatBenchmark`

## Phase 4: Build & Publish

- [x] T018 Configure `maven-publish` + `signing` in meshlink/build.gradle.kts
- [x] T019 [P] Configure XCFramework assembly (XCFrameworkConfig)
- [x] T020 [P] Create `Package.swift` (SPM binary target manifest)
- [x] T021 [P] Create `consumer-rules.pro` (ProGuard keep rules)
- [x] T022 Configure Dokka javadoc JAR generation (supports FR-001 and FR-002 publication requirements)

## Phase 5: CI Pipelines

- [x] T023 Create `ci.yml` (ktfmt, detekt, jvmTest, koverVerify, apiCheck, CI-shortened benchmark, coverage summary)
- [x] T024 [P] Create `release.yml` (publish-android, publish-ios, publish-xcframework)
- [x] T025 [P] Create `codeql.yml` (weekly security scan: actions, c-cpp, java-kotlin)
- [x] T026 [P] Configure `.githooks/pre-commit` (ktfmt + detekt; supports constitution quality gates and FR-005 CI hygiene)

## Phase 6: API Compatibility

- [x] T027 Configure BCV (JVM .api + KLib ABI tracking)
- [x] T028 Run initial `apiDump` to establish baseline (supports FR-003 and SC-003)
- [ ] T029 Configure SKIE for Swift interop (exhaustive enums, AsyncStream)
- [x] T030 Create `scripts/verify-publish.sh` (supports SC-001 and FR-010 verification)

## Phase 7: Constitution & CI Alignment

> This phase closes constitution and CI alignment for FR-005, FR-010, SC-004, and SC-005.
> Execution note: implement `T031` and `T032` before running `T033`; complete `T034` before considering FR-005 and SC-004 fully satisfied.

- [x] T031 Implement constitution-compliant `AndroidCryptoProvider` in `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/crypto/AndroidCryptoProvider.kt` behind the project-owned `CryptoProvider` abstraction
- [ ] T032 [P] Implement constitution-compliant `IosCryptoProvider` in `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoProvider.kt` behind the project-owned `CryptoProvider` abstraction
- [x] T033 Verify released Android and iOS publication outputs contain no third-party crypto binaries or native crypto payloads using `scripts/verify-publish.sh`
- [x] T034 Verify `ci.yml` runs the CI-shortened benchmark suite required by FR-005

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck
./gradlew :meshlink:jvmCiBenchmark
./gradlew :meshlink:compileKotlinIosArm64  # macOS only
```

`jvmCiBenchmark` is the verification command corresponding to the CI-shortened benchmark suite required by FR-005.

34 total tasks remain recorded, 7 have been marked removed for constitution alignment, and 25 of the 27 actionable tasks are now complete while 2 remain pending.
