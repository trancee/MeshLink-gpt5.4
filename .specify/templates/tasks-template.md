# Tasks: [FEATURE NAME]

**Input**: Design documents from `specs/[###-feature-name]/`, including active feature memory  
**Prerequisites**: plan.md (required), spec.md (required for user stories), memory-synthesis.md (required before tasking)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared state)
- **[Story]**: Which user story (US1, US2, US3…)
- Include exact file paths: `meshlink/src/commonMain/kotlin/ch/trancee/meshlink/[subsystem]/[File].kt`
- Any human or automated commit created while executing these tasks MUST use a Conventional Commit message
- Preserve relevant implementation and verification watchpoints from `memory-synthesis.md` in the task breakdown

## Verification Commands

```bash
# Full verification (must pass before merge)
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck

# Individual checks
./gradlew :meshlink:jvmTest                    # Run all tests (commonTest on JVM)
./gradlew :meshlink:koverVerify                # 100% line + branch coverage
./gradlew :meshlink:koverHtmlReport            # HTML coverage report
./gradlew :meshlink:apiCheck                   # BCV public API compatibility
./gradlew :meshlink:detekt                     # Static analysis
./gradlew :meshlink:ktfmtCheck                 # Format verification
./gradlew :meshlink:ktfmtFormat                # Auto-fix formatting

# Benchmarks (if hot-path code changed)
./gradlew :meshlink:jvmBenchmark               # Full benchmark suite
./gradlew :meshlink:jvmCiBenchmark             # CI-shortened benchmark

# iOS (macOS only)
./gradlew :meshlink:compileKotlinIos           # Verify iOS compilation
./gradlew :meshlink:apiCheck                   # Includes KLib ABI on macOS
```

<!-- 
  ============================================================================
  IMPORTANT: Replace sample tasks below with actual tasks derived from:
  - User stories from spec.md (priorities P1, P2, P3…)
  - Wire protocol impact section
  - Platform actuals section
  - Performance budget section
  
  Tasks are organized by user story for independent implementation.
  Every task MUST include tests that achieve 100% coverage of the new code.
  ============================================================================
-->

---

## Phase 1: Foundation (Shared Infrastructure)

**Purpose**: Types, interfaces, and configuration that all user stories depend on.  
**No user story work can begin until this phase completes.**

- [ ] T001 Define core types/interfaces in `meshlink/src/commonMain/.../[subsystem]/[Types].kt`
- [ ] T002 [P] Add configuration to `MeshEngineConfig` or subsystem config
- [ ] T003 [P] Add wire message type(s) in `meshlink/src/commonMain/.../wire/messages/[Msg].kt` (if applicable)
- [ ] T004 [P] Write unit tests for types + wire codec in `meshlink/src/commonTest/.../`
- [ ] T005 Run `./gradlew :meshlink:jvmTest` — new tests pass, existing tests unbroken

**Gate**: `./gradlew :meshlink:jvmTest :meshlink:detekt :meshlink:ktfmtCheck` green

---

## Phase 2: User Story 1 — [Title] (Priority: P1) 🎯 MVP

**Goal**: [What this delivers — independently verifiable]  
**Verification**: [How to prove it works via test harness]

### Implementation

- [ ] T006 Implement core logic in `meshlink/src/commonMain/.../[subsystem]/[Impl].kt`
- [ ] T007 [P] Write unit tests in `meshlink/src/commonTest/.../[subsystem]/[Impl]Test.kt`
- [ ] T008 Wire into MeshEngine in `meshlink/src/commonMain/.../engine/MeshEngine.kt`
- [ ] T009 [P] Write integration test in `meshlink/src/commonTest/.../integration/[Feature]IntegrationTest.kt`
- [ ] T010 Verify: `./gradlew :meshlink:jvmTest :meshlink:koverVerify` — 100% coverage maintained

**Checkpoint**: User Story 1 independently functional. Integration test proves end-to-end behavior.

---

## Phase 3: User Story 2 — [Title] (Priority: P2)

**Goal**: [What this delivers]  
**Verification**: [How to prove it works]

### Implementation

- [ ] T011 Implement [component] in `meshlink/src/commonMain/.../[file].kt`
- [ ] T012 [P] Write unit tests in `meshlink/src/commonTest/.../[file]Test.kt`
- [ ] T013 [P] Write integration test in `meshlink/src/commonTest/.../integration/[Test].kt`
- [ ] T014 Verify: `./gradlew :meshlink:jvmTest :meshlink:koverVerify`

**Checkpoint**: User Stories 1 and 2 both independently functional.

---

## Phase 4: User Story 3 — [Title] (Priority: P3)

**Goal**: [What this delivers]  
**Verification**: [How to prove it works]

### Implementation

- [ ] T015 Implement [component] in `meshlink/src/commonMain/.../[file].kt`
- [ ] T016 [P] Write tests in `meshlink/src/commonTest/.../[file]Test.kt`
- [ ] T017 Verify: `./gradlew :meshlink:jvmTest :meshlink:koverVerify`

**Checkpoint**: All user stories independently functional.

---

## Phase 5: Platform Actuals (if applicable)

**Purpose**: Android/iOS-specific implementations.

- [ ] T018 [P] Android actual in `meshlink/src/androidMain/.../[file].kt`
- [ ] T019 [P] iOS actual in `meshlink/src/iosMain/.../[file].kt`
- [ ] T020 [P] JVM test shim in `meshlink/src/jvmMain/.../[file].kt` (if needed)
- [ ] T021 Verify iOS compiles: `./gradlew :meshlink:compileKotlinIos`

---

## Phase 6: Public API & Documentation

**Purpose**: Expose via MeshLinkApi, update BCV dump, add docs.

- [ ] T022 Add public API surface in `meshlink/src/commonMain/.../api/[file].kt`
- [ ] T023 Update BCV dump: `./gradlew :meshlink:apiDump` and commit `.api` files
- [ ] T024 [P] Add KDoc to all public declarations
- [ ] T025 [P] Update/create documentation in `docs/[category]/[guide].md`
- [ ] T026 Verify: `./gradlew :meshlink:apiCheck`

---

## Phase 7: Performance & Polish

**Purpose**: Benchmarks, diagnostic events, final verification.

- [ ] T027 [P] Add benchmark in `meshlink/src/jvmMain/.../benchmark/[Feature]Benchmark.kt` (if hot-path)
- [ ] T028 [P] Add diagnostic emissions (if applicable) — verify against DiagnosticCode enum
- [ ] T029 Run full verification: `./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck`
- [ ] T030 [P] Run benchmarks and compare to baseline: `./gradlew :meshlink:jvmBenchmark`

**Final Gate**: All commands in "Verification Commands" section pass green.

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Foundation) ──► Phase 2 (US1/P1) ──► Phase 3 (US2/P2) ──► Phase 4 (US3/P3)
                                │                      │                      │
                                ▼                      ▼                      ▼
                         Phase 5 (Platform Actuals) ─────────────────────────►│
                                                                              ▼
                                                                   Phase 6 (API & Docs)
                                                                              │
                                                                              ▼
                                                                   Phase 7 (Perf & Polish)
```

- **Foundation (Phase 1)**: No dependencies — start immediately
- **User Stories (Phase 2–4)**: Depend on Foundation; can proceed sequentially (P1→P2→P3) or in parallel if independent
- **Platform Actuals (Phase 5)**: Can start after Foundation if actuals are simple stubs; full impl after user stories
- **API & Docs (Phase 6)**: After all user stories that affect public surface
- **Performance (Phase 7)**: Last — validates no regressions

### Parallel Opportunities

- Tasks marked `[P]` within a phase can run concurrently
- User stories can run in parallel if they touch different files (check plan.md "Files This Feature Touches")
- Unit tests and integration tests for a story can be written in parallel with implementation (TDD)

### Coverage Strategy

Per constitution: 100% line + branch coverage, no `@CoverageIgnore`.

- Write tests alongside implementation (same task or `[P]` parallel task)
- If coroutine state machine creates phantom branches, document in plan.md Complexity Tracking and add targeted Kover class exclude with inline justification
- Run `./gradlew :meshlink:koverHtmlReport` to inspect any gaps visually
