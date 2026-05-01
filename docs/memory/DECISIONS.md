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
