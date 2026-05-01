# Implementation Plan: Messaging & Delivery Pipeline

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/messaging-delivery/spec.md`  
**Status**: Migrated — implementation complete

## Summary

End-to-end message delivery with at-most-once semantics, cut-through relay forwarding for multi-hop efficiency, sliding-window rate limiting, and comprehensive diagnostic emission.

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/messaging/
├── CutThroughBuffer.kt           # FlatBuffer byte surgery for relay forwarding
├── Delivered.kt                   # Successful delivery outcome
├── DeliveryFailed.kt              # Failed delivery with reason
├── DeliveryOutcome.kt             # Sealed outcome type
├── DeliveryPipeline.kt            # Core delivery orchestrator
├── InboundMessage.kt              # Received message wrapper
├── MessageIdKey.kt                # Unique message identifier
├── MessagingConfig.kt             # Rate limits, timeouts, buffer sizes
├── PeerPair.kt                    # Sender-recipient pair key
├── QueuedReason.kt                # Why a message is queued (not sent immediately)
├── SendResult.kt                  # Immediate send outcome
└── SlidingWindowRateLimiter.kt    # Per-peer-pair rate enforcement
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Delivery semantics | At-most-once | Simplest for v1; exactly-once requires sequence dedup at app layer |
| Relay strategy | Cut-through (chunk0 forwarding) | Minimizes relay latency and memory usage |
| Rate limiting | Sliding window per peer pair | Prevents single-peer flooding without global locks |
| Message ID | Monotonic counter per sender | Simple, unique within session |
| Buffer pressure | Configurable max pending | Bounded memory; excess triggers diagnostic |
