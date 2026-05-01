# Decisions

### 2026-05-01 - Pin Kotlin to 2.3.20 until SKIE supports newer patch releases
**Status**
Active

**Why this is durable**
The iOS distribution pipeline depends on SKIE-generated Swift wrappers, so Kotlin patch upgrades can block platform-distribution work even when the rest of the build stays green.

**Decision**
Pin the repo's Kotlin version to 2.3.20 for now because SKIE 0.10.11 supports Kotlin 2.3.20 but rejects 2.3.21. Revisit the pin only after verifying a SKIE release that explicitly supports a newer Kotlin patch version.

**Tradeoffs**
This unlocks SKIE-based enum and Flow interop immediately, but it delays adoption of the newest Kotlin patch until SKIE catches up.

**Future mistake prevented**
Do not bump Kotlin independently of SKIE compatibility on this repo's iOS-distribution path.

**Evidence**
`./gradlew :meshlink:compileKotlinIosArm64 :meshlink:linkReleaseFrameworkIosArm64` failed on Kotlin 2.3.21 with an explicit SKIE compatibility error and passed after pinning Kotlin to 2.3.20.

**Where to look next**
`gradle/libs.versions.toml`, `build.gradle.kts`, `meshlink/build.gradle.kts`, `specs/platform-distribution/tasks.md`

### 2026-05-01 - Implement iOS crypto through a Swift-installed delegate bridge
**Status**
Active

**Why this is durable**
CryptoKit is Swift-only while the shared MeshLink crypto surface is defined in Kotlin, so iOS crypto needs a stable bridging pattern rather than ad hoc direct interop attempts.

**Decision**
Implement `IosCryptoProvider` as a Kotlin adapter behind `CryptoProvider` that delegates to a Swift-installed `IosCryptoDelegate` using `NSData` payloads. Keep the real cryptography in Apple-native Swift code and install the delegate through `MeshLinkIosFactory` before creating the API.

**Tradeoffs**
This keeps released artifacts constitution-compliant and lets Swift use CryptoKit directly, but it introduces a required iOS bootstrap step before MeshLink can be used.

**Future mistake prevented**
Do not try to force direct Kotlin/Native use of Swift-only CryptoKit APIs or reintroduce third-party crypto binaries just to satisfy the iOS implementation.

**Evidence**
`IosCryptoProvider` now compiles, the KLib API baseline was updated, full verification passes, and the generated iOS framework exports the delegate bridge plus SKIE-enhanced Swift wrappers.

**Where to look next**
`meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoBridge.kt`, `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/crypto/IosCryptoProvider.kt`, `meshlink/src/iosMain/kotlin/ch/trancee/meshlink/api/MeshLinkIosFactory.kt`, `docs/ios-crypto-bridge.md`

## Template
### YYYY-MM-DD - Decision title
**Status**
Active | Superseded | Needs review

**Why this is durable**
What cross-feature choice is likely to matter again?

**Decision**
What was decided and what boundary does it create?

**Tradeoffs**
What was gained, what was made harder, and when should this be reconsidered?

**Future mistake prevented**
What likely incorrect approach does this rule out?

**Evidence**
Diff, tests, review, incident, or repeated implementation evidence.

**Where to look next**
Files, modules, or specs future maintainers should inspect.
