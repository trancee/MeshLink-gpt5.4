# Implementation Plan: Platform & Distribution

**Branch**: feature branch required | **Date**: 2026-04-30 | **Spec**: `specs/platform-distribution/spec.md`  
**Status**: Migrated planning artifact — must be executed on a feature branch, not on `main`

## Summary

Platform-specific implementations (Android, iOS, JVM) plus build infrastructure for publishing to Maven Central and SPM, CI pipelines, binary compatibility validation, and SKIE Swift interop.

## Phases

1. Android platform actuals
2. iOS platform actuals
3. JVM infrastructure
4. Build and publish setup
5. CI pipelines
6. API compatibility and release validation
7. Constitution and CI alignment follow-up

## Technical Constraints

- Must comply with all constitution quality gates
- Must execute on feature branches, never directly on `main`
- Must preserve cross-platform API consistency
- Must satisfy the constitution's crypto packaging constraint
- Must keep publishing and CI verification reproducible

## Crypto Packaging Interpretation

For this feature, “constitution-compliant” platform crypto actuals means:

- released Android and iOS artifacts must not package third-party crypto binaries or native crypto payloads
- platform implementations must remain behind the project-owned `CryptoProvider` abstraction
- publication verification must explicitly confirm that no external crypto library payload is present in released artifacts

## Project Structure

```text
meshlink/src/
├── androidMain/kotlin/ch/trancee/meshlink/
│   ├── api/MeshLinkAndroidFactory.kt
│   ├── crypto/AndroidCryptoProvider.kt
│   ├── storage/AndroidSecureStorage.kt
│   └── transport/AndroidBleTransport.kt, MeshLinkService.kt, Logger.kt
├── iosMain/kotlin/ch/trancee/meshlink/
│   ├── api/MeshLinkIosFactory.kt
│   ├── crypto/IosCryptoProvider.kt
│   ├── engine/MeshNode.kt
│   ├── storage/IosSecureStorage.kt
│   └── transport/IosBleTransport.kt, Logger.kt
├── jvmMain/kotlin/ch/trancee/meshlink/
│   ├── crypto/JvmCryptoProvider.kt
│   ├── transport/Logger.kt
│   └── benchmark/*.kt (4 benchmark files)
.github/workflows/
├── ci.yml       # ktfmt → detekt → jvmTest → koverVerify → apiCheck → benchmark
├── release.yml  # publish-android → publish-ios → xcframework
└── codeql.yml   # weekly security scan
Package.swift                  # SPM binary target manifest
consumer-rules.pro             # Android consumer ProGuard rules
scripts/verify-publish.sh      # Publication verification script
.githooks/pre-commit           # Local quality gate hook
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Android crypto | Constitution-compliant Android actual via the project-owned CryptoProvider abstraction | Must satisfy the no-external-crypto-artifact constraint |
| iOS crypto | Constitution-compliant iOS actual via the project-owned CryptoProvider abstraction | Must satisfy the no-external-crypto-artifact constraint |
| JVM crypto | JDK built-in (for tests only) | No native deps needed for CI |
| Publishing | Maven Central + SPM | Standard channels for each platform |
| Signing | In-memory PGP from CI secrets | No keyring files in repo |
| SKIE | Enabled for all public API | Better Swift UX (exhaustive enums, AsyncStream) |

These platform actuals map to:
- `meshlink/src/androidMain/kotlin/ch/trancee/meshlink/crypto/AndroidCryptoProvider.kt`
- `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoProvider.kt`

Both remain behind the shared `CryptoProvider` abstraction and must satisfy the constitution's no-external-crypto-artifact constraint.

## Verification Commands

- `./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck`
- `./gradlew :meshlink:jvmCiBenchmark`
- `./gradlew :meshlink:compileKotlinIosArm64`
