# MeshLink Implementation Bootstrap Roadmap

Last updated: 2026-04-30

## Approved baseline decisions

- This repository is an **implementation-bearing repo**.
- The subsystem specs under `specs/*/spec.md` are the **normative canonical source** for detailed requirements.
- `specs/001-codebase-spec-analysis/spec.md` remains the umbrella product overview.
- The `FR-*` items in `001-codebase-spec-analysis` are **non-normative summary requirements** for overview and traceability only.
- Existing migrated `plan.md` and `tasks.md` files should be treated as **imported backlog**, not as evidence that product code already exists in this repository.

## Working implementation assumptions

- Default module root: `meshlink/`
  - Reason: all migrated plans and tasks already reference paths under `meshlink/src/...`.
  - Revisit this only with an explicit architecture decision.
- Kotlin Multiplatform remains the target architecture:
  - `commonMain`
  - `commonTest`
  - `androidMain`
  - `iosMain`
  - `jvmMain`
- Initial execution should prioritize **shared pure-Kotlin subsystems first**, then platform integration, then packaging/distribution.

## Phase 0 — Repository bootstrap

Goal: create the minimal buildable project skeleton that all subsystem work can land into.

### Deliverables

- Root Gradle bootstrap:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `gradle.properties`
  - Gradle wrapper
- Product module bootstrap:
  - `meshlink/build.gradle.kts`
  - `meshlink/src/commonMain/kotlin/...`
  - `meshlink/src/commonTest/kotlin/...`
  - `meshlink/src/androidMain/kotlin/...`
  - `meshlink/src/iosMain/kotlin/...`
  - `meshlink/src/jvmMain/kotlin/...`
- Verification baseline:
  - test task runs
  - formatting/lint placeholders wired
  - API/coverage/benchmark plugins stubbed where possible

### Exit criteria

- `./gradlew :meshlink:tasks` succeeds
- `./gradlew :meshlink:jvmTest` runs against a minimal baseline
- source set layout matches the migrated plans

## Ordered bootstrap sequence

### Wave 1 — Core primitives

#### 1. `wire-format`
Why first:
- pure Kotlin
- no platform BLE dependency
- defines binary message contracts needed by most other subsystems

Imported backlog size:
- 22 tasks from `specs/wire-format/tasks.md`

Primary outcome:
- message codec, validation, and buffer utility foundation

#### 2. `crypto-noise-protocol`
Why second:
- cryptographic identity, trust, replay protection, and handshake state are foundational
- required before secure messaging and peer trust semantics can work end-to-end

Imported backlog size:
- 31 tasks from `specs/crypto-noise-protocol/tasks.md`

Primary outcome:
- crypto provider abstraction, Noise handshake/sealing, trust store, replay guard

#### 3. `public-api`
Why third:
- establishes the public contract that later subsystems must plug into
- now the canonical source for API shape and diagnostic catalog semantics

Imported backlog size:
- 25 tasks from `specs/public-api/tasks.md`

Primary outcome:
- API surface, config DSL, diagnostics, lifecycle types, platform parity contract

### Wave 2 — Connectivity and orchestration

#### 4. `ble-transport`
Why now:
- platform transport is needed before engine-level orchestration can be exercised against real channels
- depends on core codec assumptions being settled

Imported backlog size:
- 22 tasks from `specs/ble-transport/tasks.md`

Primary outcome:
- L2CAP-first transport, GATT fallback, advertisement/discovery primitives

#### 5. `mesh-engine`
Why now:
- orchestrates crypto, transport, lifecycle, sweeps, and pseudonym rotation
- should land after core contracts and transport primitives exist

Imported backlog size:
- 22 tasks from `specs/mesh-engine/tasks.md`

Primary outcome:
- central runtime wiring and lifecycle orchestration

#### 6. `babel-routing`
Why now:
- routing logic becomes useful once engine and transport integration points exist
- depends on message identity and timing assumptions from earlier waves

Imported backlog size:
- 18 tasks from `specs/babel-routing/tasks.md`

Primary outcome:
- multi-hop routing, feasibility checks, seqno recovery, presence tracking

### Wave 3 — Delivery plane and runtime behavior

#### 7. `messaging-delivery`
Why now:
- depends on crypto, routing, and engine coordination
- expresses core user-visible messaging semantics

Imported backlog size:
- 19 tasks from `specs/messaging-delivery/tasks.md`

Primary outcome:
- at-most-once delivery, relay forwarding, rate limiting, diagnostics

#### 8. `sack-transfer`
Why now:
- builds on stable message transport and delivery behavior
- best added after the basic message path exists

Imported backlog size:
- 14 tasks from `specs/sack-transfer/tasks.md`

Primary outcome:
- chunked reliable transfer, resume, scheduling, acknowledgement handling

#### 9. `power-management`
Why now:
- depends on real transport and engine behavior to tune connection budgets and drain behavior
- easier to validate after baseline runtime loops exist

Imported backlog size:
- 15 tasks from `specs/power-management/tasks.md`

Primary outcome:
- power tier selection, parameter policy, connection limiting, graceful drain

### Wave 4 — Packaging and release surfaces

#### 10. `platform-distribution`
Why last:
- should package and verify a system that already has meaningful shared implementation
- CI, publishing, and binary compatibility checks are most useful after shared code exists

Imported backlog size:
- 30 tasks from `specs/platform-distribution/tasks.md`

Primary outcome:
- Android/iOS/JVM platform actuals, publishing, CI, BCV, SKIE, release pipeline

## First execution slice

Recommended first slice:

1. Finish **Phase 0 repository bootstrap**
2. Implement the first thin vertical slice of **`wire-format`**

That first slice should aim for:
- a compilable `meshlink` module
- one minimal message type
- encode/decode round-trip tests in `commonTest`
- baseline verification command(s) that can run in CI later

## Immediate next actions

1. Bootstrap Gradle + KMP project structure under `meshlink/`
2. Import `wire-format` as the first active implementation backlog
3. Reinterpret migrated tasks as executable backlog items before claiming completion
4. Re-run drift/sync analysis after the first real product code lands
