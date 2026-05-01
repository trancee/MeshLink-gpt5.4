# Implementation Plan: SACK Transfer

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/sack-transfer/spec.md`  
**Status**: Migrated — implementation complete

## Summary

Reliable chunked transfer with TCP SACK-inspired selective acknowledgment, priority-based scheduling, resume-after-disconnect, and adaptive rate control.

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/transfer/
├── ChunkSizePolicy.kt            # GATT vs L2CAP chunk sizing
├── FailureReason.kt              # Transfer failure reasons
├── ObservationRateController.kt  # Adaptive send rate
├── Priority.kt                   # HIGH/NORMAL/LOW scheduling priority
├── ResumeCalculator.kt           # Resume offset from SACK state
├── SackTracker.kt                # Selective acknowledgment tracking
├── TransferConfig.kt             # Timeouts, retransmit limits
├── TransferEngine.kt             # Core transfer orchestrator
├── TransferEvent.kt              # Progress/Complete/Failed events
├── TransferScheduler.kt          # Priority-based scheduling
└── TransferSession.kt            # Per-transfer state machine
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| ACK strategy | SACK (RFC 2018-inspired) | Retransmit only missing chunks, not entire window |
| Chunk sizing | ChunkSizePolicy (GATT/L2CAP) | Adapts to transport capability |
| Rate control | Observation-based | No explicit congestion signal in BLE; infer from ACK rate |
| Resume | Offset-based from SACK state | Minimal metadata to persist across reconnects |
| Scheduling | Priority enum | Simple; covers interactive vs bulk distinction |
