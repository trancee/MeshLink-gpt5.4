# Tasks: SACK Transfer

**Status**: Complete — transfer foundation, engine, and benchmark implemented

## Phase 1: Foundation

- [x] T001 Define `TransferConfig` (timeouts, retransmit limits, window size)
- [x] T002 [P] Define `Priority` enum (HIGH, NORMAL, LOW)
- [x] T003 [P] Define `ChunkSizePolicy` (GATT vs L2CAP sizing)
- [x] T004 [P] Define `FailureReason`, `TransferEvent` types
- [x] T005 Write `TransferFoundationTest`

## Phase 2: Transfer Core

- [x] T006 Implement `SackTracker` (selective acknowledgment state)
- [x] T007 Implement `TransferSession` (per-transfer state machine: chunking, acking, completing)
- [x] T008 Implement `TransferEngine` (orchestrator: send, receive, retransmit, complete)
- [x] T009 [P] Implement `ObservationRateController` (adaptive rate from ACK timing)
- [x] T010 [P] Implement `ResumeCalculator` (offset from SACK state after disconnect)
- [x] T011 [P] Implement `TransferScheduler` (priority-ordered queue)
- [x] T012 Write `TransferSessionTest`
- [x] T013 Write `TransferEngineTest` — multi-chunk, retransmit, complete

## Phase 3: Benchmark

- [x] T014 Add `TransferBenchmark` — chunk processing throughput

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:jvmBenchmark
```

14 actionable tasks complete, 0 remain pending.
