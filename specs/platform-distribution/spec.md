# Feature Specification: Platform & Distribution

**Status**: Migrated  
**Spec References**: spec/03-transport-ble.md, spec/12-platform-and-testing.md, spec/13-distribution-api-compliance.md  
**Subsystem**: Platform source sets + build infrastructure

## User Scenarios & Testing

### User Story 1 - Android Distribution (Priority: P1)

MeshLink ships as an AAR published to Maven Central for Android consumers.

**Acceptance Scenarios**:

1. **Given** an Android app developer, **When** they add `implementation("ch.trancee:meshlink:0.1.0")`, **Then** the AAR resolves with all transitive deps
2. **Given** a release tag push, **When** CI runs release.yml, **Then** Android + JVM artifacts are published to OSSRH

### User Story 2 - iOS Distribution (Priority: P1)

MeshLink ships as an XCFramework distributed via Swift Package Manager.

**Acceptance Scenarios**:

1. **Given** an iOS developer, **When** they add the SPM package URL, **Then** MeshLink.xcframework resolves
2. **Given** a release tag, **When** CI builds on macOS, **Then** XCFramework is assembled and attached to GitHub release
3. **Given** SKIE is enabled, **When** Swift consumers call the API, **Then** enums are exhaustive, Flows are AsyncSequence

### User Story 3 - Binary Compatibility (Priority: P1)

Public API is tracked by BCV to prevent accidental breaking changes.

**Acceptance Scenarios**:

1. **Given** a public API change, **When** apiCheck runs, **Then** it fails until apiDump is explicitly run
2. **Given** iOS KLib, **When** built on macOS, **Then** KLib ABI is also validated

### User Story 4 - CI Quality Gates (Priority: P1)

Every PR must pass lint, test, coverage, and API check before merge.

**Acceptance Scenarios**:

1. **Given** a PR to main, **When** CI runs, **Then** ktfmt, detekt, jvmTest, koverVerify, apiCheck all pass
2. **Given** a security vulnerability in a dependency, **When** CodeQL scans weekly, **Then** alert is raised

## Requirements

- **FR-001**: Android artifact MUST be published as AAR to Maven Central
- **FR-002**: iOS MUST be distributed as static XCFramework via SPM binary target
- **FR-003**: BCV MUST track JVM .api file + iOS KLib ABI (macOS only)
- **FR-004**: SKIE MUST generate Swift-friendly wrappers (exhaustive enums, AsyncStream)
- **FR-005**: CI MUST run `ktfmt`, `detekt`, `jvmTest`, `koverVerify`, `apiCheck`, and the CI-shortened benchmark suite in `ci.yml`
- **FR-006**: Release MUST run: publish-android → publish-ios → publish-xcframework (release.yml)
- **FR-007**: CodeQL MUST scan weekly (actions, c-cpp, java-kotlin)
- **FR-008**: Signing MUST use in-memory PGP keys from CI secrets
- **FR-009**: ProGuard consumer rules MUST be shipped alongside AAR
- **FR-010**: Platform packaging MUST comply with the constitution's crypto constraint: no external crypto library may ship in the released artifact

## Platform Actuals

| Platform | Files | Purpose |
|----------|-------|---------|
| androidMain | AndroidCryptoProvider, AndroidBleTransport, MeshLinkService, AndroidSecureStorage, MeshLinkAndroidFactory | Full Android implementation |
| iosMain | IosCryptoProvider, IosBleTransport, IosSecureStorage, MeshLinkIosFactory, MeshNode | Full iOS implementation |
| jvmMain | JvmCryptoProvider, benchmarks | Test infrastructure + performance measurement |

> `AndroidCryptoProvider` and `IosCryptoProvider` are constitution-compliant platform actual names. Their implementations must remain behind the shared `CryptoProvider` abstraction and must not ship third-party crypto binaries in released artifacts.

## Success Criteria

- **SC-001**: `./gradlew :meshlink:publishAllPublicationsToOSSRHRepository` succeeds (dry-run)
- **SC-002**: XCFramework assembles on macOS
- **SC-003**: BCV apiCheck passes on all targets
- **SC-004**: `ci.yml` passes on pull requests, `release.yml` passes on release-tag workflows, and `codeql.yml` completes successfully on its scheduled run
- **SC-005**: SKIE verification confirms exhaustive enum bridging for `MeshLinkState` and async/flow interop for one public stream-based API exposed to Swift consumers
