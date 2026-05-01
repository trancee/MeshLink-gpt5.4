# Feature Specification: Wire Format & Codec

**Status**: Migrated  
**Spec References**: spec/04-wire-format.md  
**Subsystem**: `wire/`, `wire/messages/`

## User Scenarios & Testing

### User Story 1 - Binary Message Encoding/Decoding (Priority: P1)

All mesh protocol messages are encoded to compact binary format for BLE transmission and decoded on receipt.

**Independent Test**: `WireFormatTest` — round-trip encode/decode for all message types

**Acceptance Scenarios**:

1. **Given** any protocol message, **When** encoded via WireCodec, **Then** produces a ByteArray with MessageType tag + payload
2. **Given** a valid encoded ByteArray, **When** decoded via WireCodec, **Then** reconstructs the original message
3. **Given** a truncated or malformed buffer, **When** decode is attempted, **Then** returns a ValidationResult.Invalid

### User Story 2 - Inbound Message Validation (Priority: P1)

All inbound messages are validated before processing (length bounds, field constraints, hop limits).

**Independent Test**: `InboundValidatorTest` — boundary conditions, oversized payloads, invalid fields

**Acceptance Scenarios**:

1. **Given** a message exceeding max payload size, **When** validated, **Then** rejected with appropriate code
2. **Given** a routed message with hop count at limit, **When** validated, **Then** rejected (HOP_LIMIT_EXCEEDED)
3. **Given** a well-formed message within bounds, **When** validated, **Then** accepted for processing

### User Story 3 - Buffer Utilities (Priority: P2)

Efficient read/write buffers for zero-copy binary encoding without external dependencies.

**Independent Test**: `ReadWriteBufferTest` — int/long/byte array serialization, bounds checking

## Requirements

- **FR-001**: WireCodec MUST support all 11+ message types (Hello, Handshake, Update, Chunk, ChunkAck, DeliveryAck, Nack, Keepalive, Broadcast, RoutedMessage, ResumeRequest, RotationAnnouncement)
- **FR-002**: Encoding MUST be deterministic (same input → same bytes)
- **FR-003**: MessageType tag MUST be 1 byte (compact for BLE MTU)
- **FR-004**: InboundValidator MUST reject messages exceeding configured bounds
- **FR-005**: ReadBuffer/WriteBuffer MUST handle little-endian encoding without allocations in hot path

## Wire Protocol Impact

This IS the wire protocol. All message types defined here.

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Encode/decode | <1μs per message | WireFormatBenchmark |
| Allocations | 0 in ReadBuffer hot path | Structural inspection |

## Success Criteria

- **SC-001**: All 11+ message types round-trip correctly
- **SC-002**: InboundValidator catches all boundary violations
- **SC-003**: Benchmark: <1μs encode+decode per message
- **SC-004**: 100% line and branch coverage
