# Feature Specification: Messaging & Delivery Pipeline

**Status**: Migrated  
**Spec References**: spec/09-messaging.md  
**Subsystem**: `messaging/`

## User Scenarios & Testing

### User Story 1 - End-to-End Message Delivery (Priority: P1)

Application messages are delivered reliably from sender to recipient with confirmation.

**Independent Test**: `DeliveryPipelineTest` — send, receive, acknowledge cycle

**Acceptance Scenarios**:

1. **Given** a message to a reachable peer, **When** sent via DeliveryPipeline, **Then** the message is delivered and DeliveryAck received
2. **Given** a message to an unreachable peer, **When** delivery times out, **Then** `DeliveryFailed` is emitted with appropriate reason
3. **Given** an in-flight message, **When** cancelled by the sender, **Then** resources are released and no DeliveryAck is expected

### User Story 2 - Cut-Through Relay Forwarding (Priority: P1)

Intermediate nodes forward messages without buffering the full payload — relay the first chunk immediately while routing header is intact.

**Independent Test**: `CutThroughBufferTest`, `DeliveryPipelineCutThroughTest`

**Acceptance Scenarios**:

1. **Given** a multi-hop message arriving at a relay, **When** chunk0 contains routing header, **Then** the relay forwards immediately without waiting for full message
2. **Given** a relay forwarding a message, **When** visited_hops list is updated via FlatBuffer byte surgery, **Then** the appended hop is visible to downstream nodes

### User Story 3 - Rate Limiting (Priority: P2)

Prevent flooding by rate-limiting message sends per peer pair.

**Independent Test**: `MessagingFoundationTest` — SlidingWindowRateLimiter behavior

### User Story 4 - Delivery Outcome Mapping (Priority: P2)

Map internal delivery states to public API outcomes (Delivered, Failed with reason).

**Independent Test**: `DeliveryOutcomeMapperTest`

## Requirements

- **FR-001**: DeliveryPipeline MUST guarantee at-most-once delivery semantics
- **FR-002**: Cut-through relay MUST forward chunk0 within 1 hop without full buffering
- **FR-003**: SlidingWindowRateLimiter MUST enforce configurable per-peer-pair limits
- **FR-004**: Message ID MUST be unique per sender (monotonic counter)
- **FR-005**: DeliveryPipeline MUST emit diagnostics for timeout, buffer pressure, cancellation

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Relay forwarding latency | <1ms overhead per hop | Integration test |
| Rate limiter check | O(1) | Structural inspection |

## Success Criteria

- **SC-001**: Messages delivered end-to-end in MeshTestHarness topologies
- **SC-002**: Cut-through relay verified in CutThroughRelayIntegrationTest
- **SC-003**: Rate limiter correctly throttles burst traffic
- **SC-004**: 100% line and branch coverage
