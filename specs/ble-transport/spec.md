# Feature Specification: BLE Transport

**Status**: Migrated  
**Spec References**: spec/03-transport-ble.md  
**Subsystem**: `transport/`

## User Scenarios & Testing

### User Story 1 - L2CAP Data Transfer (Priority: P1)

Primary data channel uses BLE L2CAP CoC for high-throughput, low-latency communication.

**Independent Test**: `L2capFrameCodecTest` — frame encoding/decoding, fragmentation

**Acceptance Scenarios**:

1. **Given** two connected peers, **When** data is sent via L2CAP, **Then** frames are encoded with length prefix and delivered in order
2. **Given** an L2CAP connection failure, **When** retry is attempted, **Then** L2capRetryScheduler manages exponential backoff

### User Story 2 - BLE Advertisement & Discovery (Priority: P1)

Nodes advertise their presence using BLE advertisements with encoded pseudonym and power tier.

**Independent Test**: `AdvertisementCodecTest` — encode/decode service data

**Acceptance Scenarios**:

1. **Given** a node's pseudonym and power tier, **When** encoded into advertisement, **Then** fits within BLE advertisement size limits
2. **Given** a received advertisement, **When** decoded, **Then** extracts pseudonym and power tier correctly

### User Story 3 - GATT Fallback (Priority: P2)

When L2CAP is unavailable (older devices, OEM restrictions), fall back to GATT characteristic writes.

**Independent Test**: `OemL2capProbeCacheTest`, `ConnectionInitiationPolicyTest`

### User Story 4 - Mesh Hash Filtering (Priority: P2)

Filter duplicate advertisements from the same mesh using a hash-based approach.

**Independent Test**: `MeshHashFilterTest`

## Requirements

- **FR-001**: BleTransport interface MUST abstract L2CAP and GATT behind a common API
- **FR-002**: L2capFrameCodec MUST handle frame encoding with length prefix
- **FR-003**: L2capRetryScheduler MUST implement exponential backoff with jitter
- **FR-004**: AdvertisementCodec MUST encode pseudonym + power tier in ≤31 bytes
- **FR-005**: ConnectionInitiationPolicy MUST determine which peer initiates (deterministic from peer IDs)
- **FR-006**: OemL2capProbeCache MUST cache L2CAP capability per device model
- **FR-007**: GattConstants MUST define service/characteristic UUIDs

## Platform Actuals

| Platform | Source Set | Implementation |
|----------|-----------|----------------|
| Common | `commonMain/transport/` | BleTransport interface, codecs, policies |
| Android | `androidMain/transport/` | AndroidBleTransport (BluetoothGatt, L2CAP), MeshLinkService |
| iOS | `iosMain/transport/` | IosBleTransport (CoreBluetooth, CBL2CAPChannel) |

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| L2CAP throughput | ≥80 KB/s Android, ≥60 KB/s iOS | Hardware integration test |
| Advertisement parse | <10μs | Unit test timing |

## Success Criteria

- **SC-001**: L2capFrameCodec round-trips all frame types
- **SC-002**: AdvertisementCodec fits within BLE limits
- **SC-003**: ConnectionInitiationPolicy is deterministic and symmetric
- **SC-004**: OemL2capProbeCache correctly caches device capabilities
- **SC-005**: 100% coverage on commonMain transport code
