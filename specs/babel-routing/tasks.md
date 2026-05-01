# Tasks: Babel Routing

**Status**: In progress — routing foundation types bootstrapped

## Phase 1: Foundation

- [x] T001 Define `RoutingConfig` (timeouts, thresholds, hop limit)
- [x] T002 [P] Define `PeerInfo` data class (neighbor state)
- [x] T003 [P] Define `OutboundFrame` (queued transmission frame)
- [x] T004 Write `RoutingConfigTest`

## Phase 2: Routing Core

- [x] T005 Implement `RoutingTable` (route storage, metric comparison, best-route selection)
- [ ] T006 Implement `RoutingEngine` (Babel update processing, route installation, withdrawal)
- [ ] T007 Implement `RouteCoordinator` (seqno management, feasibility conditions, starvation recovery)
- [x] T008 Write `RoutingTableTest` — multi-route scenarios, metric ordering
- [ ] T009 Write `RoutingEngineTest` — update processing, convergence
- [ ] T010 Write `RouteCoordinatorSeqNoTest` — feasibility, starvation
- [ ] T011 Write `RouteCoordinatorDiagnosticTest` — diagnostic emissions
- [ ] T012 Write `RoutingTest` — end-to-end routing scenarios

## Phase 3: Presence & Dedup

- [ ] T013 Implement `PresenceTracker` (timeout-based presence, events)
- [ ] T014 [P] Implement `DedupSet` (LRU-bounded deduplication)
- [ ] T015 Write `PresenceTrackerTest`
- [ ] T016 Write `DedupSetTest`

## Phase 4: Benchmarks

- [ ] T017 [P] Add `RoutingBenchmark` — route lookup throughput
- [ ] T018 [P] Add `DedupBenchmark` — dedup check throughput

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:jvmBenchmark
```

6 actionable tasks complete, 12 remain pending.
