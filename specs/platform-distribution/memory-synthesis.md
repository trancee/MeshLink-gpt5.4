# Memory Synthesis

feature: platform-distribution
status: complete
hard_conflicts: 0
soft_conflicts: 0
assumptions_to_confirm: 0

## Current Constraints
- [C1] Read `.specify/memory/constitution.md` and `docs/memory/*` before continuing work on this feature.
- [C2] iOS BLE distribution is physical-device only; simulator execution is intentionally out of scope.
- [C3] All dependency and plugin version changes must stay in `gradle/libs.versions.toml` and use `version.ref`.
- [C4] Release artifacts must not ship third-party crypto binaries or native crypto payloads.

## Reused Decisions
- [D1] Keep Kotlin pinned to the current SKIE-compatible version until a newer supported pairing is verified (`docs/memory/DECISIONS.md`).
- [D2] Implement iOS crypto through the Swift-installed delegate bridge behind `IosCryptoProvider` (`docs/memory/DECISIONS.md`).

## Relevant Bug Patterns
- [none]

## Architecture Boundaries
- [A1] Shared logic stays in `commonMain`; platform source sets contain actual implementations and platform glue.
- [A2] SwiftPM distribution requires a release-hosted XCFramework zip plus checksum wired into `Package.swift`.

## Feature-to-Memory Conflicts
- [none]

## Assumptions Requiring Confirmation
- [none]

## Implementation Watchpoints
- [W1] Keep tasks specific, file-scoped, and parallel only when files do not overlap.
- [W2] Do not add simulator-based iOS BLE verification.

## Verification Watchpoints
- [V1] Run the full Gradle verification gate plus `jvmCiBenchmark` before completion claims.
- [V2] Validate release artifacts with `scripts/verify-publish.sh`.
- [V3] Validate the SwiftPM checksum against the produced `MeshLink.xcframework.zip` on macOS.
- [V4] Keep `Package.swift` aligned with the tagged release URL and checksum before publishing.
