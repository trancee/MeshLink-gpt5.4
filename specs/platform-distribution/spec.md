# Feature Specification: Platform & Distribution

**Status**: Refreshed after pre-implementation review  
**Spec References**: `spec/03-transport-ble.md`, `spec/12-platform-and-testing.md`, `spec/13-distribution-api-compliance.md`  
**Subsystem**: Platform source sets + build infrastructure

## User Scenarios & Testing

### User Story 1 - Android Distribution (Priority: P1)

MeshLink ships as an AAR published to Maven Central for Android consumers.

**Acceptance Scenarios**:

1. **Given** an Android app developer, **When** they add `implementation("ch.trancee:meshlink:0.1.0")`, **Then** the AAR resolves with all transitive dependencies and consumer ProGuard rules.
2. **Given** a release tag push, **When** `release.yml` runs, **Then** Android + JVM publications are uploaded to OSSRH using in-memory signing credentials.

### User Story 2 - iOS Device Distribution (Priority: P1)

MeshLink ships as a static XCFramework distributed via a Swift Package Manager binary target for **physical iOS devices**.

**Acceptance Scenarios**:

1. **Given** an iOS developer targeting a physical iPhone or iPad, **When** they add the Swift package URL, **Then** `MeshLink.xcframework` resolves from the release-hosted binary target.
2. **Given** a release tag, **When** CI runs on macOS, **Then** the workflow assembles `MeshLink.xcframework`, zips it, computes the SwiftPM checksum, updates or validates `Package.swift`, and attaches the zip to the GitHub release.
3. **Given** SKIE is enabled, **When** Swift consumers call the API from a physical-device build, **Then** closed enums remain exhaustive and one public stream-based API is surfaced as `AsyncSequence`.
4. **Given** a simulator-only workflow, **When** a developer evaluates MeshLink BLE support, **Then** the documentation and packaging make it explicit that simulator execution is out of scope because BLE transport support is device-only.

### User Story 3 - Binary Compatibility (Priority: P1)

Public API is tracked by BCV to prevent accidental breaking changes on JVM and iOS.

**Acceptance Scenarios**:

1. **Given** a public API change, **When** `apiCheck` runs, **Then** it fails until `apiDump` is explicitly run and the baseline is reviewed.
2. **Given** an iOS KLib build on macOS, **When** `apiCheck` runs, **Then** the KLib ABI baseline is also validated.

### User Story 4 - CI Quality Gates (Priority: P1)

Every PR must pass lint, test, coverage, benchmarks, API checks, and release-path verification before merge.

**Acceptance Scenarios**:

1. **Given** a PR to `main`, **When** `ci.yml` runs, **Then** `ktfmt`, `detekt`, `jvmTest`, `androidHostTest`, `koverVerify`, `apiCheck`, and the CI-shortened benchmark suite all pass.
2. **Given** a release-tag workflow, **When** publication artifacts are produced, **Then** `scripts/verify-publish.sh` confirms no forbidden third-party crypto payloads are packaged.
3. **Given** a vulnerable dependency or workflow action, **When** `codeql.yml` runs weekly, **Then** an alert is raised.

## Requirements

- **FR-001**: Android artifacts MUST be published as AAR/JVM publications to Maven Central.
- **FR-002**: iOS MUST be distributed as a static XCFramework through a SwiftPM binary target for **physical-device BLE integrations**.
- **FR-003**: Simulator execution is explicitly out of scope for BLE acceptance; iOS verification MUST use macOS/device-only build validation.
- **FR-004**: BCV MUST track `meshlink/api/jvm/meshlink.api` and `meshlink/api/meshlink.klib.api`.
- **FR-005**: SKIE MUST generate Swift-friendly wrappers for the public API, including exhaustive enum bridging and `AsyncSequence` interop.
- **FR-006**: `ci.yml` MUST run `ktfmt`, `detekt`, `jvmTest`, `androidHostTest`, `koverVerify`, `apiCheck`, and `jvmCiBenchmark`.
- **FR-007**: `release.yml` MUST run `publish-android` → `publish-ios` → `publish-xcframework`, where the XCFramework stage assembles the release XCFramework, zips it, computes the SwiftPM checksum, validates `Package.swift`, and publishes the release asset.
- **FR-008**: `codeql.yml` MUST scan weekly for `actions`, `c-cpp`, and `java-kotlin`.
- **FR-009**: Signing MUST use in-memory PGP keys from CI secrets.
- **FR-010**: `consumer-rules.pro` MUST ship alongside Android artifacts.
- **FR-011**: Platform packaging MUST comply with the constitution's crypto constraint: no external crypto library may ship in the released artifact.
- **FR-012**: `Package.swift` MUST declare a `binaryTarget` whose URL and checksum point to the GitHub-release-hosted `MeshLink.xcframework.zip`.
- **FR-013**: Dependency and plugin version changes for this feature MUST be declared in `gradle/libs.versions.toml` using `version.ref` rather than inline version literals.

## Platform Actuals

| Platform | Files | Purpose |
|----------|-------|---------|
| `androidMain` | `AndroidCryptoProvider`, `AndroidBleTransport`, `MeshLinkService`, `AndroidSecureStorage`, `MeshLinkAndroidFactory` | Full Android implementation |
| `iosMain` | `IosCryptoBridge`, `IosCryptoProvider`, `IosBleTransport`, `IosSecureStorage`, `MeshLinkIosFactory`, `MeshNode` | Device-only iOS implementation |
| `jvmMain` | `JvmCryptoProvider`, benchmark shims | Test infrastructure + performance measurement |

> `AndroidCryptoProvider` and `IosCryptoProvider` are constitution-compliant platform actual names. Their implementations must remain behind the shared `CryptoProvider` abstraction and must not ship third-party crypto binaries in released artifacts.

## Success Criteria

- **SC-001**: `./gradlew :meshlink:publishAllPublicationsToOSSRHRepository` succeeds in dry-run-ready local verification.
- **SC-002**: `./gradlew :meshlink:assembleMeshLinkReleaseXCFramework` succeeds on macOS and produces the release XCFramework zip consumed by SwiftPM packaging.
- **SC-003**: `./gradlew :meshlink:apiCheck` passes for JVM and iOS baselines.
- **SC-004**: `ci.yml` passes on pull requests, `release.yml` passes on release-tag workflows, and `codeql.yml` completes successfully on its schedule.
- **SC-005**: SKIE verification confirms exhaustive enum bridging for `MeshLinkState` and `AsyncSequence` interop for at least one public stream-based API exposed to Swift consumers on the device-only iOS path.
- **SC-006**: `Package.swift` references a release-hosted `MeshLink.xcframework.zip` URL and checksum that match the produced artifact.
