# Implementation Plan: Babel Routing

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/babel-routing/spec.md`  
**Status**: Migrated — implementation complete

## Summary

Distance-vector routing based on RFC 8966 (Babel), adapted for BLE mesh. Supports multi-hop route discovery, feasibility-condition loop prevention, seqno-based starvation recovery, presence tracking, and flood deduplication.

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/routing/
├── DedupSet.kt            # LRU-bounded message deduplication
├── OutboundFrame.kt       # Frame queued for transmission
├── PeerInfo.kt            # Neighbor information record
├── PresenceTracker.kt     # Timeout-based peer presence detection
├── RouteCoordinator.kt    # Seqno management, starvation recovery
├── RoutingConfig.kt       # Configurable timeouts and thresholds
├── RoutingEngine.kt       # Core Babel routing logic
└── RoutingTable.kt        # Route storage with metric comparison
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Protocol base | Babel (RFC 8966) | Loop-free, proven for wireless, supports metric-based selection |
| Metric | Hop count (simple) | BLE links are roughly equal quality; ETX unnecessary for v1 |
| Feasibility | Seqno-based (Babel standard) | Prevents loops without full path info |
| Dedup | Time-bounded LRU set | Bounded memory, O(1) lookup |
| Presence | Timeout-based with configurable interval | Simple, works across power tiers |
