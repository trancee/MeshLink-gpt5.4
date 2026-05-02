# Platform Distribution Remediation

This note captures the maintainer-facing release and verification contract added
by the umbrella remediation wave.

## What changed

The repository now treats platform distribution hardening as a first-class part
of release verification:

- `.github/workflows/codeql.yml` scans `actions`, `c-cpp`, and `java-kotlin`.
- `.github/workflows/ci.yml` runs the explicit remediation quality gate tasks
  instead of hiding them behind an opaque workflow-only contract.
- `.github/workflows/release.yml` now generates a release-specific `Package.swift`
  manifest, validates it against the computed XCFramework checksum, and uploads
  that manifest alongside the XCFramework assets.
- `Package.swift` in the repository is now a concrete manifest instead of a
  placeholder checksum stub.

## CodeQL cadence and scope

`codeql.yml` is the long-running security watchpoint for release hardening.

It runs on:

- pull requests
- pushes to `main`
- a weekly schedule (`23 4 * * 1`)
- manual `workflow_dispatch`

The workflow scans these GitHub-supported surfaces:

- `actions`
- `c-cpp`
- `java-kotlin`

When changing workflow files, native packaging glue, or Kotlin build logic, keep
those surfaces in mind and treat CodeQL success as part of the ship gate rather
than as a best-effort signal.

## CI quality gate contract

The PR workflow must continue to enforce these Gradle checks explicitly:

```bash
./gradlew \
  :meshlink:ktfmtCheck \
  :meshlink:detekt \
  :meshlink:jvmTest \
  :meshlink:androidHostTest \
  :meshlink:koverVerify \
  :meshlink:apiCheck \
  :meshlink:jvmCiBenchmark
```

`meshlink/build.gradle.kts` keeps `ciQualityGate` aligned with that same task
set so local dry runs and workflow runs do not drift.

## Release checksum update flow

The SwiftPM binary target must always reference the exact release-hosted
XCFramework zip and its matching checksum.

### Local dry-run sequence

Run the release-shaped path on macOS:

```bash
./gradlew :meshlink:assembleMeshLinkReleaseXCFramework
./scripts/package-xcframework.sh
./scripts/update-package-swift.sh \
  --version v0.1.0 \
  --checksum "$(cat meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip.checksum)" \
  --repository trancee/MeshLink-gpt5.4
./scripts/update-package-swift.sh \
  --verify \
  --version v0.1.0 \
  --checksum "$(cat meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip.checksum)" \
  --repository trancee/MeshLink-gpt5.4
./scripts/verify-publish.sh \
  --expected-package-url "https://github.com/trancee/MeshLink-gpt5.4/releases/download/v0.1.0/MeshLink.xcframework.zip" \
  --expected-package-checksum "$(cat meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip.checksum)" \
  meshlink/build/XCFrameworks/release
```

### Workflow contract

The release workflow owns the final release metadata path:

1. assemble the release XCFramework on macOS
2. package the XCFramework zip and compute the SwiftPM checksum
3. generate `meshlink/build/XCFrameworks/release/Package.swift`
4. verify that generated manifest against the computed checksum and repository
   slug
5. verify packaged artifacts with `scripts/verify-publish.sh`
6. publish the zip, checksum file, and generated `Package.swift` as release
   assets

If the checksum changes, regenerate the manifest. Never edit the checksum by
hand.

## Why macOS-only release verification matters

The iOS packaging path is device-only and depends on Apple toolchains.
Non-Apple environments can validate shared Kotlin code, but they cannot provide
trustworthy evidence for the final XCFramework packaging path.

Use macOS for:

- `:meshlink:compileKotlinIosArm64`
- `:meshlink:assembleMeshLinkReleaseXCFramework`
- `./scripts/package-xcframework.sh`
- release-manifest generation and verification

Linux CI remains the primary shared-code quality lane, but Apple packaging must
be proven on macOS before release completion.

## SKIE verification expectations

Watchpoint `[V2]` requires explicit evidence that the Apple packaging path still
preserves the Swift ergonomics promised by SKIE.

Before shipping a release, validate at least these two behaviors from a
physical-device iOS integration path:

1. `MeshLinkState` bridges as an exhaustive Swift enum, so `switch` statements
   do not need a `default` case.
2. At least one public stream API — currently `diagnosticEvents` — bridges as an
   `AsyncSequence` and can be consumed with `for await`.

If either behavior regresses:

- rebuild the release framework on macOS
- inspect the generated Swift interface
- block the release until the SKIE bridge is corrected

For the Swift-side examples and crypto-delegate install order, see
`docs/ios-crypto-bridge.md`.

## API-facing operational notes

The remediation wave also finalized a small set of public operational APIs that
matter during release validation and support handoff:

- `healthSnapshot()` returns a point-in-time summary of connected peers, routing
  entries, active transfers, buffered delivery state, and the currently applied
  power tier.
- `forgetPeer(peerId)` erases runtime state for one peer without resetting the
  full mesh instance. Peer-scoped routes, active transfers, and buffered
  deliveries for that peer are cleared.
- `factoryReset()` is intentionally stricter: callers must stop the runtime
  first, then the reset clears all in-memory runtime state.

These semantics should remain stable across Android and iOS because they are now
part of the shared `MeshLinkApi` contract and BCV-tracked public surface.
