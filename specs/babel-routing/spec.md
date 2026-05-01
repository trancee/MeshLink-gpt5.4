# Feature Specification: Babel Routing

**Status**: Migrated  
**Spec References**: spec/07-routing.md  
**Subsystem**: `routing/`

## User Scenarios & Testing

### User Story 1 - Multi-hop Route Discovery (Priority: P1)

Nodes discover reachable peers through Babel distance-vector routing updates, building a routing table that enables multi-hop message delivery.

**Independent Test**: `RoutingEngineTest`, `RoutingTableTest` — route installation, metric comparison, next-hop selection

**Acceptance Scenarios**:

1. **Given** a routing update from a neighbor, **When** the advertised route is better than current, **Then** the route is installed with updated metric
2. **Given** multiple routes to the same destination, **When** next-hop is queried, **Then** the lowest-metric route is selected
3. **Given** a route withdrawal (metric=infinity), **When** processed, **Then** the route is removed and alternatives are selected

### User Story 2 - Loop Prevention (Priority: P1)

Babel feasibility conditions prevent routing loops even during convergence.

**Independent Test**: `RouteCoordinatorSeqNoTest` — seqno-based feasibility, starvation recovery

**Acceptance Scenarios**:

1. **Given** a route with seqno less than the source table entry, **When** feasibility is checked, **Then** the route is rejected (would create loop)
2. **Given** route starvation (no feasible route), **When** seqno request is sent, **Then** the destination increments seqno and re-advertises

### User Story 3 - Presence Tracking (Priority: P2)

Track which peers are currently reachable (within BLE range or via multi-hop).

**Independent Test**: `PresenceTrackerTest` — timeout-based presence, appearance/disappearance events

### User Story 4 - Flood Deduplication (Priority: P2)

Prevent broadcast storms by deduplicating flooded messages.

**Independent Test**: `DedupSetTest` — insertion, expiry, capacity eviction

## Requirements

- **FR-001**: RoutingEngine MUST implement Babel distance-vector with feasibility conditions
- **FR-002**: RoutingTable MUST support multiple routes per destination with metric comparison
- **FR-003**: RouteCoordinator MUST handle seqno requests for starvation recovery
- **FR-004**: PresenceTracker MUST emit peer appeared/disappeared events with configurable timeout
- **FR-005**: DedupSet MUST deduplicate with bounded memory (LRU eviction)
- **FR-006**: Routing convergence MUST complete within 3 seconds for 10-node topology changes

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Route lookup | O(1) per destination | RoutingBenchmark |
| Dedup check | O(1) amortized | DedupBenchmark |
| Convergence | <3s for 10-node change | Integration test |

## Success Criteria

- **SC-001**: Routes converge correctly in multi-hop topologies (MeshTestHarness)
- **SC-002**: Feasibility conditions prevent all routing loops
- **SC-003**: Presence events fire within configured timeout
- **SC-004**: DedupSet never exceeds capacity bound
- **SC-005**: 100% line and branch coverage
