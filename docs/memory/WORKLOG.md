# Worklog

Use concise high-value entries only.
This is not a changelog.

### 2026-05-01 - Kotlin downgrade unlocks SKIE
- why this is durable: SKIE compatibility now constrains Kotlin patch upgrades for the iOS distribution path.
- what future mistake it prevents: upgrading Kotlin past the highest SKIE-supported patch without verifying iOS framework generation first.
- evidence: full verification passed after pinning Kotlin to 2.3.20, and SKIE generated `MeshLinkState` enum wrappers plus `SkieSwiftStateFlow`/`SkieSwiftSharedFlow` AsyncSequence wrappers in the iOS framework output.
- where future contributors should look: `gradle/libs.versions.toml`, `meshlink/build.gradle.kts`, `meshlink/build/bin/iosArm64/releaseFramework/MeshLink.framework/Modules/MeshLink.swiftmodule/arm64-apple-ios.swiftinterface`

## Template
### YYYY-MM-DD - Summary
- why this is durable
- what future mistake it prevents
- evidence
- where future contributors should look
