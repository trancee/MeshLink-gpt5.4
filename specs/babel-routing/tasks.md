# Tasks: Babel Routing

**Status**: Complete — routing core, presence, dedup, and benchmarks implemented

## Phase 1: Foundation

- [x] T001 Define `RoutingConfig` (timeouts, thresholds, hop limit)
- [x] T002 [P] Define `PeerInfo` data class (neighbor state)
- [x] T003 [P] Define `OutboundFrame` (queued transmission frame)
- [x] T004 Write `RoutingConfigTest`

## Phase 2: Routing Core

- [x] T005 Implement `RoutingTable` (route storage, metric comparison, best-route selection)
- [x] T006 Implement `RoutingEngine` (Babel update processing, route installation, withdrawal)
- [x] T007 Implement `RouteCoordinator` (seqno management, feasibility conditions, starvation recovery)
- [x] T008 Write `RoutingTableTest` — multi-route scenarios, metric ordering
- [x] T009 Write `RoutingEngineTest` — update processing, convergence
- [x] T010 Write `RouteCoordinatorSeqNoTest` — feasibility, starvation
- [x] T011 Write `RouteCoordinatorDiagnosticTest` — diagnostic emissions
- [x] T012 Write `RoutingTest` — end-to-end routing scenarios

## Phase 3: Presence & Dedup

- [x] T013 Implement `PresenceTracker` (timeout-based presence, events)
- [x] T014 [P] Implement `DedupSet` (LRU-bounded deduplication)
- [x] T015 Write `PresenceTrackerTest`
- [x] T016 Write `DedupSetTest`

## Phase 4: Benchmarks

- [x] T017 [P] Add `RoutingBenchmark` — route lookup throughput
- [x] T018 [P] Add `DedupBenchmark` — dedup check throughput

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:jvmBenchmark
```

18 actionable tasks complete, 0 remain pending.
