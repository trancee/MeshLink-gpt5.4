# Feature Memory

## Scope Notes
- Refreshed planning context for `platform-distribution` on 2026-05-01.
- Current work is in closeout: `spec.md`, `plan.md`, and `tasks.md` now reflect an implemented and verified feature.
- iOS BLE distribution is intentionally **physical-device only**; simulator execution is out of scope for this feature.

## Relevant Durable Memory
- Read `.specify/memory/constitution.md` first.
- Read `docs/memory/PROJECT_CONTEXT.md`, `ARCHITECTURE.md`, `DECISIONS.md`, and `BUGS.md` before resuming work.
- Honor the active repo decision to keep Kotlin pinned to the SKIE-compatible version until compatibility changes are verified.
- Honor the active repo decision to keep iOS crypto behind the `IosCryptoDelegate` / `IosCryptoProvider` bridge rather than shipping third-party crypto binaries.

## Open Questions
- Confirm the final release-asset hosting URL pattern used by `Package.swift` for the remote `binaryTarget`.
- Confirm whether a dedicated helper script or workflow step will own checksum insertion into `Package.swift`.

## Watchlist
- Keep all dependency and plugin version changes in `gradle/libs.versions.toml` using `version.ref`.
- Keep iOS verification device-only; do not add simulator-based BLE acceptance tasks.
- Keep publication verification explicit so release artifacts are checked for forbidden crypto payloads.
- Capture only durable, evidenced lessons back into `docs/memory/*`.
