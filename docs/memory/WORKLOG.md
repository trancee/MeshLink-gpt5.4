# Worklog

Use concise high-value entries only.
This is not a changelog.

### 2026-05-01 - Kotlin downgrade unlocks SKIE
- why this is durable: SKIE compatibility now constrains Kotlin patch upgrades for the iOS distribution path.
- what future mistake it prevents: upgrading Kotlin past the highest SKIE-supported patch without verifying iOS framework generation first.
- evidence: full verification passed after pinning Kotlin to 2.3.20, and SKIE generated `MeshLinkState` enum wrappers plus `SkieSwiftStateFlow`/`SkieSwiftSharedFlow` AsyncSequence wrappers in the iOS framework output.
- where future contributors should look: `gradle/libs.versions.toml`, `meshlink/build.gradle.kts`, `meshlink/build/bin/iosArm64/releaseFramework/MeshLink.framework/Modules/MeshLink.swiftmodule/arm64-apple-ios.swiftinterface`

### 2026-05-01 - Added iOS crypto delegate bridge
- why this is durable: iOS cryptography must stay Apple-native while the shared MeshLink API remains in Kotlin.
- what future mistake it prevents: attempting direct CryptoKit interop from Kotlin or hiding iOS bootstrap requirements.
- evidence: full verification, updated KLib API dump, and new bridge files under `iosMain` plus `docs/ios-crypto-bridge.md`.
- where future contributors should look: `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoBridge.kt`, `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoProvider.kt`, `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/api/MeshLinkIosFactory.kt`

## Template
### YYYY-MM-DD - Summary
- why this is durable
- what future mistake it prevents
- evidence
- where future contributors should look
