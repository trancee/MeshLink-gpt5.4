# Implementation Plan: Platform & Distribution

**Branch**: `feat/platform-distribution` | **Date**: 2026-05-01 | **Spec**: `specs/platform-distribution/spec.md`  
**Status**: Refreshed after pre-implementation review

## Summary

Deliver platform-specific Android, iOS, and JVM implementations plus a reproducible distribution pipeline for Maven Central and SwiftPM. The iOS path is intentionally **physical-device only** for BLE usage; simulator execution is not a supported acceptance path. Build configuration changes must preserve constitution compliance and keep all dependency/plugin version declarations centralized in `gradle/libs.versions.toml` with `version.ref`.

## Phases

1. Build governance and shared publishing foundation
2. Android distribution implementation
3. iOS physical-device distribution implementation
4. JVM benchmark and compatibility infrastructure
5. Release asset packaging and manifests
6. CI / release automation
7. Acceptance verification and constitution closure

## Technical Constraints

- Must comply with all constitution quality gates.
- Must execute on a feature branch, never directly on `main`.
- Must preserve cross-platform API consistency.
- Must satisfy the constitution's crypto packaging constraint.
- Must keep publishing and CI verification reproducible.
- iOS BLE validation is **device-only**; simulator execution is intentionally unsupported.
- All dependency and plugin version changes must flow through `gradle/libs.versions.toml` using `version.ref` entries.
- Kotlin must remain pinned to the SKIE-compatible version tracked in `docs/memory/DECISIONS.md` until a newer supported pairing is verified.

## Crypto Packaging Interpretation

For this feature, “constitution-compliant” platform crypto actuals means:

- released Android and iOS artifacts must not package third-party crypto binaries or native crypto payloads
- platform implementations must remain behind the project-owned `CryptoProvider` abstraction
- publication verification must explicitly confirm that no external crypto library payload is present in released artifacts

## Project Structure

```text
gradle/
└── libs.versions.toml
build.gradle.kts
meshlink/
├── api/
│   ├── jvm/meshlink.api
│   └── meshlink.klib.api
├── build.gradle.kts
└── src/
    ├── androidMain/kotlin/ch/trancee/meshlink/
    │   ├── api/MeshLinkAndroidFactory.kt
    │   ├── crypto/AndroidCryptoProvider.kt
    │   ├── storage/AndroidSecureStorage.kt
    │   └── transport/AndroidBleTransport.kt, MeshLinkService.kt, Logger.kt
    ├── androidHostTest/kotlin/ch/trancee/meshlink/
    │   ├── api/MeshLinkAndroidFactoryTest.kt
    │   └── crypto/AndroidCryptoProviderTest.kt
    ├── iosMain/kotlin/ch/trancee/meshlink/
    │   ├── api/MeshLinkIosFactory.kt
    │   ├── crypto/IosCryptoBridge.kt, IosCryptoProvider.kt
    │   ├── engine/MeshNode.kt
    │   ├── storage/IosSecureStorage.kt
    │   └── transport/IosBleTransport.kt, Logger.kt
    ├── jvmBenchmark/kotlin/ch/trancee/meshlink/
    │   ├── routing/DedupBenchmark.kt
    │   ├── routing/RoutingBenchmark.kt
    │   ├── transfer/TransferBenchmark.kt
    │   └── wire/WireFormatBenchmark.kt
    └── jvmMain/kotlin/ch/trancee/meshlink/
        ├── crypto/JvmCryptoProvider.kt
        └── transport/Logger.kt
.github/workflows/
├── ci.yml
├── codeql.yml
└── release.yml
.githooks/pre-commit
Package.swift
consumer-rules.pro
scripts/
├── package-xcframework.sh
├── update-package-swift.sh
└── verify-publish.sh
docs/ios-crypto-bridge.md
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Version management | Use `gradle/libs.versions.toml` + `version.ref` for every dependency/plugin version touched by this feature | Keeps build changes auditable and consistent with repository policy |
| Android crypto | Constitution-compliant Android actual via the project-owned `CryptoProvider` abstraction | Must satisfy the no-external-crypto-artifact constraint |
| iOS crypto | Constitution-compliant iOS actual via the `IosCryptoBridge` / `IosCryptoProvider` path behind `CryptoProvider` | Keeps released artifacts compliant while using Apple-native crypto on device |
| iOS packaging | Static XCFramework for physical devices only | BLE is not a supported simulator acceptance path |
| SwiftPM distribution | Release-hosted `MeshLink.xcframework.zip` + checksum referenced by `Package.swift` | Required by SwiftPM `binaryTarget` remote distribution |
| Publishing | Maven Central + GitHub release assets | Matches Android/JVM and iOS distribution channels |
| Signing | In-memory PGP from CI secrets | No keyring files in repo |
| SKIE | Enabled for public API with explicit verification of enum + stream interop | Better Swift UX without diverging the Kotlin API |

## Requirement Coverage

| Requirement | Planned Coverage |
|-------------|------------------|
| FR-001, FR-009, FR-010 | `meshlink/build.gradle.kts`, `consumer-rules.pro`, `release.yml`, `verify-publish.sh` |
| FR-002, FR-003, FR-012 | `meshlink/build.gradle.kts`, `Package.swift`, `scripts/package-xcframework.sh`, `scripts/update-package-swift.sh`, `release.yml`, `docs/ios-crypto-bridge.md` |
| FR-004 | `build.gradle.kts`, `meshlink/api/jvm/meshlink.api`, `meshlink/api/meshlink.klib.api` |
| FR-005 | `meshlink/build.gradle.kts`, SKIE verification step, `docs/ios-crypto-bridge.md` |
| FR-006, FR-007, FR-008 | `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `.github/workflows/codeql.yml` |
| FR-011 | `AndroidCryptoProvider.kt`, `IosCryptoProvider.kt`, `verify-publish.sh`, release artifact inspection |
| FR-013 | `gradle/libs.versions.toml`, `build.gradle.kts`, `meshlink/build.gradle.kts` |

## Verification Commands

- `./gradlew :meshlink:ktfmtCheck :meshlink:detekt :meshlink:jvmTest :meshlink:androidHostTest :meshlink:koverVerify :meshlink:apiCheck`
- `./gradlew :meshlink:jvmCiBenchmark`
- `./gradlew :meshlink:compileKotlinIosArm64 :meshlink:assembleMeshLinkReleaseXCFramework`
- `./scripts/verify-publish.sh meshlink/build/outputs/aar meshlink/build/XCFrameworks/release`
- `swift package compute-checksum meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip` *(macOS only, after packaging)*
