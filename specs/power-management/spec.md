# Feature Specification: Power Management

**Status**: Migrated  
**Spec References**: spec/10-power-and-presence.md  
**Subsystem**: `power/`

## User Scenarios & Testing

### User Story 1 - Power Tier Adaptation (Priority: P1)

The mesh adapts its radio behavior based on battery state (HIGH/NORMAL/LOW tiers).

**Independent Test**: `PowerManagerTest` — tier transitions, policy enforcement

**Acceptance Scenarios**:

1. **Given** battery at 80%, **When** PowerManager evaluates, **Then** HIGH tier is active (aggressive scanning)
2. **Given** battery at 30%, **When** PowerManager evaluates, **Then** NORMAL tier is active (balanced)
3. **Given** battery at 10%, **When** PowerManager evaluates, **Then** LOW tier is active (minimal scan, long intervals)

### User Story 2 - Connection Limiting (Priority: P1)

Limit concurrent connections based on power tier to conserve battery.

**Independent Test**: Verified via `PowerManagerTest` — connection count enforcement per tier

**Acceptance Scenarios**:

1. **Given** LOW power tier, **When** max connections reached, **Then** new connections are rejected
2. **Given** tier upgrade (LOW→NORMAL), **When** connection limit increases, **Then** queued connections are allowed

### User Story 3 - Graceful Drain (Priority: P2)

When battery critically low, gracefully drain existing connections rather than abruptly disconnecting.

**Independent Test**: `GracefulDrainIntegrationTest`

**Acceptance Scenarios**:

1. **Given** battery dropping below drain threshold, **When** drain is initiated, **Then** in-flight transfers complete before disconnection
2. **Given** drain in progress, **When** all transfers complete, **Then** connections are closed cleanly

## Requirements

- **FR-001**: PowerManager MUST evaluate battery level and select appropriate PowerTier
- **FR-002**: PowerProfile MUST define scan intervals, connection limits, and duty cycles per tier
- **FR-003**: ConnectionLimiter MUST enforce max concurrent connections per tier
- **FR-004**: GracefulDrainManager MUST allow in-flight transfers to complete before disconnect
- **FR-005**: BleConnectionParameterPolicy MUST adjust BLE connection parameters per tier
- **FR-006**: In LOW tier, scan duty cycle MUST NOT exceed 5%

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Tier evaluation | <1ms | Unit test |
| Battery poll interval | Configurable (default 60s) | PowerConfig |
| LOW tier scan duty | ≤5% | BleConnectionParameterPolicy |

## Success Criteria

- **SC-001**: Tier transitions fire at correct battery thresholds
- **SC-002**: Connection limits enforced per tier
- **SC-003**: Graceful drain completes in-flight transfers (integration test)
- **SC-004**: 100% line and branch coverage
