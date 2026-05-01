# Tasks: SACK Transfer

**Status**: Migrated — task tracking reset; all actionable tasks pending

## Phase 1: Foundation

- [ ] T001 Define `TransferConfig` (timeouts, retransmit limits, window size)
- [ ] T002 [P] Define `Priority` enum (HIGH, NORMAL, LOW)
- [ ] T003 [P] Define `ChunkSizePolicy` (GATT vs L2CAP sizing)
- [ ] T004 [P] Define `FailureReason`, `TransferEvent` types
- [ ] T005 Write `TransferFoundationTest`

## Phase 2: Transfer Core

- [ ] T006 Implement `SackTracker` (selective acknowledgment state)
- [ ] T007 Implement `TransferSession` (per-transfer state machine: chunking, acking, completing)
- [ ] T008 Implement `TransferEngine` (orchestrator: send, receive, retransmit, complete)
- [ ] T009 [P] Implement `ObservationRateController` (adaptive rate from ACK timing)
- [ ] T010 [P] Implement `ResumeCalculator` (offset from SACK state after disconnect)
- [ ] T011 [P] Implement `TransferScheduler` (priority-ordered queue)
- [ ] T012 Write `TransferSessionTest`
- [ ] T013 Write `TransferEngineTest` — multi-chunk, retransmit, complete

## Phase 3: Benchmark

- [ ] T014 Add `TransferBenchmark` — chunk processing throughput

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:jvmBenchmark
```

All 14 actionable tasks are now pending.
