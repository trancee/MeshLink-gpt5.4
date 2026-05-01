# Feature Specification: SACK Transfer

**Status**: Migrated  
**Spec References**: spec/08-transfer.md  
**Subsystem**: `transfer/`

## User Scenarios & Testing

### User Story 1 - Chunked Reliable Transfer (Priority: P1)

Large payloads are split into chunks, transmitted with selective acknowledgment, and reassembled at the receiver.

**Independent Test**: `TransferEngineTest` — multi-chunk send, SACK receipt, retransmission

**Acceptance Scenarios**:

1. **Given** a payload exceeding chunk size, **When** sent via TransferEngine, **Then** it is split into chunks and transmitted in order
2. **Given** a lost chunk, **When** SACK indicates gap, **Then** only the missing chunk is retransmitted
3. **Given** all chunks acknowledged, **When** transfer completes, **Then** TransferEvent.Complete is emitted

### User Story 2 - Transfer Resume (Priority: P2)

After a disconnection, transfers resume from the last acknowledged byte offset rather than restarting.

**Independent Test**: `TransferSessionTest` — resume calculation after reconnect

### User Story 3 - Priority Scheduling (Priority: P2)

Multiple concurrent transfers are scheduled by priority (interactive > bulk).

**Independent Test**: `TransferFoundationTest` — priority ordering, scheduler behavior

## Requirements

- **FR-001**: TransferEngine MUST split payloads into chunks respecting ChunkSizePolicy
- **FR-002**: SackTracker MUST track selective acknowledgments per transfer
- **FR-003**: ResumeCalculator MUST compute correct resume offset from SACK state
- **FR-004**: TransferScheduler MUST order transfers by Priority (HIGH > NORMAL > LOW)
- **FR-005**: ObservationRateController MUST adapt send rate to observed throughput

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Chunk processing | <100μs per chunk | TransferBenchmark |
| SACK tracking | O(1) per ack | Structural inspection |
| Resume calculation | O(n) where n = gap count | Unit test |

## Success Criteria

- **SC-001**: Multi-chunk transfers complete reliably (all chunks delivered)
- **SC-002**: SACK correctly identifies gaps for selective retransmission
- **SC-003**: Resume produces correct offset after simulated disconnect
- **SC-004**: Priority scheduling respects ordering
- **SC-005**: 100% line and branch coverage
