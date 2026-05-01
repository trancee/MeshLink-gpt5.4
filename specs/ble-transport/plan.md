# Implementation Plan: BLE Transport

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/ble-transport/spec.md`  
**Status**: Migrated — implementation complete

## Summary

BLE transport layer with L2CAP-first data channel, GATT fallback, advertisement-based discovery, deterministic connection initiation, and OEM compatibility probing.

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transport/
├── AdvertisementCodec.kt          # Pseudonym + power tier encoding
├── BleTransport.kt                # Abstract transport interface
├── BleTransportConfig.kt          # Scan intervals, timeouts
├── ConnectionInitiationPolicy.kt  # Deterministic initiator selection
├── GattConstants.kt               # Service/characteristic UUIDs
├── L2capFrameCodec.kt             # Length-prefixed frame encoding
├── L2capRetryScheduler.kt         # Exponential backoff with jitter
├── Logger.kt                      # expect/actual platform logger
├── MeshHashFilter.kt              # Advertisement dedup filter
├── OemL2capProbeCache.kt          # Device model → L2CAP capability cache
├── OemSlotTracker.kt              # BLE advertisement slot management
└── WriteLatencyTracker.kt         # Write latency measurement
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Primary channel | L2CAP CoC | 3-10× throughput over GATT |
| Fallback | GATT characteristic writes | Compatibility with older devices |
| Connection initiation | Deterministic from sorted peer IDs | Avoids simultaneous connection attempts |
| Retry | Exponential backoff + jitter | Standard congestion avoidance |
| OEM compatibility | Probe cache per device model | Some OEMs don't support L2CAP; cache avoids repeated probes |
