# Feature Memory

## Scope Notes
- Cross-spec remediation wave that closes the biggest gaps still visible between the canonical subsystem specs and the current codebase.
- Intended to drive one umbrella plan, then concrete follow-up tasks across transport, crypto, engine, routing, messaging, transfer, API, and distribution.

## Relevant Durable Memory
- Kotlin is pinned to 2.3.20 until SKIE supports newer patch releases (`docs/memory/DECISIONS.md`).
- iOS crypto must continue using the Swift-installed delegate bridge; do not introduce third-party crypto binaries.
- Constitution requires canonical multi-node integration tests to use `MeshTestHarness` + `VirtualMeshTransport`.

## Open Questions
- Whether health snapshot / forgetPeer / factoryReset should be added in this umbrella wave directly or split into a follow-up feature after planning.
- Whether any gap closure will require a wire-shape change, or whether behavior-only remediation is sufficient.

## Watchlist
- Real BLE transport work can expand quickly if platform behavior is not constrained before tasks are written.
- BCV baselines and Package.swift checksum validation will need explicit handling if public API or release metadata changes.

## Never Store Here
- permanent project decisions
- general bug patterns unless they are directly reused
- implementation history after the feature ships
