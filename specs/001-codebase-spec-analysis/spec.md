# Feature Specification: MeshLink — Encrypted Serverless BLE Mesh SDK

**Feature Branch**: `001-codebase-spec-analysis`  
**Created**: 2026-04-30  
**Status**: Draft  
**Input**: User description: "Analyze the codebase and create a spec out of it"

## Specification Ownership

This document is the umbrella product overview for MeshLink.

Detailed subsystem requirements are owned by the subsystem specs:

- BLE transport → `specs/ble-transport/spec.md`
- Crypto, trust, and handshakes → `specs/crypto-noise-protocol/spec.md`
- Routing → `specs/babel-routing/spec.md`
- Engine orchestration → `specs/mesh-engine/spec.md`
- Messaging and delivery → `specs/messaging-delivery/spec.md`
- Large payload transfer and resume → `specs/sack-transfer/spec.md`
- Public API and diagnostics → `specs/public-api/spec.md`
- Power management → `specs/power-management/spec.md`
- Wire format and codec → `specs/wire-format/spec.md`
- Platform distribution, CI, and publishing → `specs/platform-distribution/spec.md`

The functional requirements in this document are non-normative summary requirements for product overview and traceability.
If any detailed requirement in this overview conflicts with a subsystem spec, the subsystem spec is canonical.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Peer Discovery and Secure Connection (Priority: P1)

As a mobile app developer, I want my app to automatically discover nearby devices running the same app and establish a secure, encrypted connection — without requiring internet, servers, or user accounts — so that users can communicate in offline environments.

**Why this priority**: Discovery and secure connection are the foundational capabilities. Without them, no other feature (messaging, routing, transfers) can function.

**Independent Test**: Can be fully tested by launching two devices in proximity and verifying that each reports the other as "Found" with a completed mutual authentication handshake, and that the connection is end-to-end encrypted.

**Acceptance Scenarios**:

1. **Given** two devices with the same app ID are within BLE range, **When** both start the mesh library, **Then** each device emits a "Peer Found" event within 10 seconds containing the peer's unique identity.
2. **Given** two devices attempt to connect, **When** the mutual authentication handshake completes, **Then** both peers verify each other's identity cryptographically and pin the key on first contact (TOFU).
3. **Given** a previously trusted peer presents a different identity key, **When** the library detects the change in Strict mode, **Then** the connection is silently rejected; in Prompt mode, the app is notified to resolve the key change.
4. **Given** a device is within BLE range, **When** it uses a different app ID, **Then** it is never discovered or connected to (mesh isolation).

---

### User Story 2 — Encrypted Unicast Messaging (Priority: P1)

As a mobile app developer, I want to send end-to-end encrypted messages to a specific peer, with delivery confirmation and store-and-forward buffering, so that users can communicate securely even when the recipient is temporarily unreachable.

**Why this priority**: 1:1 encrypted messaging is the primary use case. It directly enables "chat without internet" and is the core value proposition.

**Independent Test**: Can be fully tested by sending a message from Device A to Device B and verifying it arrives intact, encrypted, and with a delivery confirmation returned to the sender.

**Acceptance Scenarios**:

1. **Given** two connected peers, **When** Peer A sends a payload to Peer B, **Then** Peer B receives the exact payload within 50 ms (single hop, 256 bytes), and the message is end-to-end encrypted.
2. **Given** the recipient is temporarily unreachable, **When** a message is sent, **Then** the message is buffered locally and delivered when the peer becomes reachable again (store-and-forward).
3. **Given** a message is successfully received and decrypted, **When** the recipient processes it, **Then** a signed delivery confirmation is returned to the sender.
4. **Given** an attacker replays a previously captured message, **When** the library receives it, **Then** the message is silently dropped (replay protection).

---

### User Story 3 — Multi-Hop Mesh Routing (Priority: P1)

As a mobile app developer, I want messages to be automatically routed through intermediate devices when the sender and recipient are not within direct BLE range, so that the effective communication range extends beyond a single BLE hop.

**Why this priority**: Multi-hop routing is what makes this a "mesh" network rather than a simple point-to-point BLE connection. It dramatically extends reach and is a core differentiator.

**Independent Test**: Can be tested with a 3-node topology (A↔B↔C, where A cannot directly reach C) and verifying that a message from A arrives at C via B.

**Acceptance Scenarios**:

1. **Given** a linear topology A↔B↔C where A and C are out of range, **When** A sends a message to C, **Then** B automatically relays the message and C receives it successfully decrypted.
2. **Given** multiple paths exist, **When** the routing table converges, **Then** the lowest-cost route is selected within 3 seconds for a 10-node topology change.
3. **Given** a route to a peer becomes unavailable, **When** the routing table is updated, **Then** alternative routes are discovered automatically and messages are rerouted.
4. **Given** a message has already been forwarded, **When** it arrives at a relay node a second time, **Then** it is deduplicated and not forwarded again.

---

### User Story 4 — Broadcast Messaging (Priority: P2)

As a mobile app developer, I want to broadcast a signed message to all reachable peers within a configurable hop radius, so that users can publish presence, profiles, or announcements to everyone nearby.

**Why this priority**: Broadcast enables discovery use cases (e.g., profile sharing in proximity social apps) and is the mechanism for flood-fill presence. Lower priority than unicast since it builds upon routing.

**Independent Test**: Can be tested by broadcasting a payload and verifying all reachable peers within the configured hop limit receive it exactly once.

**Acceptance Scenarios**:

1. **Given** a mesh of 5 devices, **When** one device broadcasts a payload with maxHops=3, **Then** all devices within 3 hops receive the payload exactly once.
2. **Given** a broadcast message, **When** it is received by any peer, **Then** the Ed25519 signature is verified to confirm the sender's identity.
3. **Given** an unsigned broadcast arrives, **When** the library is configured to require signatures, **Then** the broadcast is rejected.
4. **Given** a broadcast with TTL=0 arrives at a relay, **When** the relay processes it, **Then** it is not forwarded further.

---

### User Story 5 — Large File Transfer with Reliability (Priority: P2)

As a mobile app developer, I want to send large payloads (up to 100 KB) reliably over the mesh with progress tracking and automatic resume after disconnections, so that users can share files, images, or profiles without worrying about connection drops.

**Why this priority**: Large payload support enables rich media sharing, which is critical for social and collaboration use cases. Built on top of core messaging.

**Independent Test**: Can be tested by sending a 50 KB payload, simulating a mid-transfer disconnection, and verifying the transfer resumes and completes without retransmitting already-acknowledged chunks.

**Acceptance Scenarios**:

1. **Given** a 50 KB payload, **When** sent to a connected peer, **Then** progress events are emitted showing bytes transferred, and the peer receives the complete payload.
2. **Given** a transfer in progress, **When** the BLE connection drops and reconnects, **Then** the transfer resumes from the last acknowledged byte offset.
3. **Given** multiple transfers in progress, **When** they have different priorities, **Then** higher-priority transfers receive proportionally more bandwidth.
4. **Given** no chunk acknowledgement arrives within 30 seconds, **When** the timeout fires, **Then** the transfer is marked as failed and a failure event is emitted.

---

### User Story 6 — Battery-Aware Power Management (Priority: P2)

As a mobile app developer, I want the library to automatically adjust its radio usage based on battery level so that my app does not excessively drain the user's battery, while still maintaining mesh connectivity.

**Why this priority**: Power management is critical for user adoption on mobile devices. Without it, the library would drain batteries rapidly and be unusable in production.

**Independent Test**: Can be tested by simulating battery level changes and verifying the library transitions between power tiers, adjusting scan duty cycle, connection limits, and chunk sizes accordingly.

**Acceptance Scenarios**:

1. **Given** the battery is above 80%, **When** the library is running, **Then** it operates in Performance mode with maximum scan duty and connection capacity.
2. **Given** the battery drops below 30%, **When** the threshold is crossed with hysteresis, **Then** the library enters Power Saver mode within 30 seconds, reducing scan duty to ≤5% and connection intervals to ≥500 ms.
3. **Given** the device begins charging, **When** the charger is connected, **Then** the library immediately transitions to Performance mode regardless of battery level.
4. **Given** a mode downgrade occurs, **When** the connection limit decreases, **Then** lowest-priority connections are evicted after a 30-second grace period for in-flight transfers.

---

### User Story 7 — Cross-Platform Identical Behavior (Priority: P2)

As a mobile app developer targeting both Android and iOS, I want a single API that works identically on both platforms, so that I don't need to learn or maintain platform-specific integration code.

**Why this priority**: Cross-platform consistency is a core product promise. Divergent behavior between platforms would undermine the library's value proposition.

**Independent Test**: Can be tested by running the same integration test scenario on both platforms and verifying identical event sequences, state transitions, and message delivery behavior.

**Acceptance Scenarios**:

1. **Given** the same configuration DSL, **When** used on Android and iOS, **Then** both platforms produce identical behavior for discovery, messaging, and routing.
2. **Given** a diagnostic event occurs, **When** observed on either platform, **Then** the same diagnostic code, severity, and payload structure is emitted.
3. **Given** a lifecycle state transition, **When** triggered on either platform, **Then** the same state machine sequence occurs (UNINITIALIZED → RUNNING → PAUSED → STOPPED).
4. **Given** an error condition, **When** it occurs on either platform, **Then** the same exception type is thrown (no platform-specific exceptions leak to consumers).

---

### User Story 8 — Privacy and Tracking Prevention (Priority: P3)

As a mobile app developer, I want the library to prevent BLE tracking of my users by rotating advertising identifiers periodically, so that third parties cannot correlate a user's location over time by observing their BLE advertisements.

**Why this priority**: Privacy is important but builds on top of the identity system. It's an enhancement to the core connection model rather than a prerequisite.

**Independent Test**: Can be tested by observing BLE advertisements over time and verifying the advertised pseudonym changes every 15 minutes while connected peers can still verify the pseudonym belongs to the same identity.

**Acceptance Scenarios**:

1. **Given** a device is advertising, **When** 15 minutes elapse, **Then** the advertised pseudonym rotates to a new value derived from the identity but unlinkable to the previous one.
2. **Given** a connected peer, **When** a pseudonym rotation occurs, **Then** the connected peer can verify the new pseudonym belongs to the same identity (HMAC verification with ±1 epoch tolerance).
3. **Given** a passive observer, **When** monitoring BLE advertisements over 1 hour, **Then** they cannot correlate different pseudonyms to the same physical device.

---

### User Story 9 — Observability and Diagnostics (Priority: P3)

As a mobile app developer, I want a structured diagnostic event stream with health snapshots so that I can monitor mesh network health, debug connectivity issues, and understand system behavior without reading logs.

**Why this priority**: Diagnostics are essential for production debugging but are not required for core functionality.

**Independent Test**: Can be tested by triggering known events (connection, disconnection, handshake failure) and verifying the corresponding diagnostic events are emitted with correct codes, severity, and payload.

**Acceptance Scenarios**:

1. **Given** the library is running, **When** a diagnostic-worthy event occurs, **Then** a structured event with code, severity, timestamp, and typed payload is emitted on the diagnostic stream.
2. **Given** the library is running, **When** a health snapshot is requested or the periodic interval elapses, **Then** a snapshot containing connected peers, routing table size, buffer usage, active transfers, and power mode is returned.
3. **Given** high event throughput, **When** the diagnostic buffer overflows, **Then** oldest events are dropped and the next emitted event includes a dropped-count indicator.
4. **Given** GDPR mode is enabled, **When** diagnostic events include peer identifiers, **Then** they are truncated/redacted in all diagnostic output.

---

### User Story 10 — GDPR Data Erasure (Priority: P3)

As a mobile app developer, I want to erase all locally stored data for a specific peer or perform a complete factory reset, so that my app can comply with GDPR right-to-erasure requirements.

**Why this priority**: Regulatory compliance is important but is an operational concern rather than a core messaging feature.

**Independent Test**: Can be tested by calling forgetPeer, then verifying the trust store, replay counters, buffered messages, and routing entries for that peer are completely erased.

**Acceptance Scenarios**:

1. **Given** a known peer, **When** `forgetPeer` is called, **Then** all trust store entries, replay counters, rotation nonces, buffered messages, and routing entries for that peer are erased.
2. **Given** a running mesh, **When** `factoryReset` is called (after stop), **Then** all identity keys, trust store, and persistent state are permanently erased.
3. **Given** a forgotten peer reappears, **When** they attempt to connect, **Then** the library treats them as a brand-new peer (fresh TOFU).

---

### Edge Cases

- What happens when BLE is disabled by the OS at runtime? → The library transitions to RECOVERABLE state and resumes when BLE is re-enabled.
- What happens when the store-and-forward buffer is full and a new message arrives? → Lowest-priority messages are evicted (relay first, then own outbound) following the 3-tier eviction policy.
- What happens when two devices simultaneously try to connect to each other? → A deterministic tie-breaking rule (higher key hash = Central role) prevents duplicate connections.
- What happens when the maximum hop count is reached during relay? → The message is dropped at the hop limit; no further forwarding occurs.
- What happens when a device experiences memory pressure? → `shedMemoryPressure()` evicts low-priority buffers and cancels below-threshold transfers.
- What happens when L2CAP connection fails? → Automatic fallback to GATT transport with degraded throughput.
- What happens when a device is on the EU regulatory region? → Advertisement interval is clamped to ≥300 ms and scan duty cycle to ≤70% (ETSI EN 300 328 compliance).

## Requirements *(mandatory)*

### Functional Requirements

> These requirements are non-normative summary requirements for end-to-end product overview and traceability.
> Detailed normative ownership lives in the subsystem specs listed in **Specification Ownership** above.
> If this overview conflicts with a subsystem spec, the subsystem spec is canonical.

- **FR-001**: System MUST discover peers within BLE range that share the same application identifier, without requiring internet connectivity, servers, or user accounts.
- **FR-002**: System MUST perform mutual cryptographic authentication (Noise XX handshake) with every discovered peer before any application data is exchanged.
- **FR-003**: System MUST encrypt all unicast messages end-to-end using authenticated encryption (Noise K with ChaCha20-Poly1305), ensuring only the intended recipient can decrypt.
- **FR-004**: System MUST route messages through intermediate peers when the sender and recipient are not within direct BLE range (multi-hop mesh routing).
- **FR-005**: System MUST converge routing tables within 3 seconds for topology changes up to 10 nodes (Babel-based distance-vector routing).
- **FR-006**: System MUST provide store-and-forward buffering for messages destined to temporarily unreachable peers, with configurable buffer capacity and priority-based eviction.
- **FR-007**: System MUST support broadcast messaging with Ed25519 signatures, configurable hop radius, and per-message deduplication.
- **FR-008**: System MUST support chunked transfer of large payloads (up to 100 KB) with selective acknowledgement (SACK), progress tracking, and byte-offset resume after disconnection.
- **FR-009**: System MUST provide three battery-adaptive power tiers (Performance, Balanced, Power Saver) that automatically adjust scan duty cycle, connection limits, and chunk sizes based on battery level.
- **FR-010**: System MUST expose a reactive event stream for peer discovery, message receipt, delivery confirmations, transfer progress, transfer failures, key changes, and diagnostic events.
- **FR-011**: System MUST protect user privacy by rotating BLE advertising pseudonyms every 15 minutes using HMAC-derived identifiers that connected peers can verify.
- **FR-012**: System MUST prevent message replay attacks using a 64-bit sliding window per sender.
- **FR-013**: System MUST support identity key rotation with signed announcements propagated to the mesh, invalidating existing sessions and requiring new handshakes.
- **FR-014**: System MUST support GDPR data erasure: per-peer forget (erases all local state) and full factory reset.
- **FR-015**: System MUST support lifecycle management (start, stop, pause, resume) with a deterministic state machine (UNINITIALIZED → RUNNING → PAUSED → STOPPED, plus RECOVERABLE and TERMINAL error states).
- **FR-016**: System MUST prefer L2CAP transport for data transfer (3–10× throughput over GATT) with automatic fallback to GATT when L2CAP is unavailable.
- **FR-017**: System MUST provide an identical API surface on Android and iOS with platform differences confined to internal implementations.
- **FR-018**: System MUST support configurable rate limiting for outbound unicast messages, broadcasts, and handshake initiations.
- **FR-019**: System MUST provide health snapshots including connected peers, routing table size, buffer usage, active transfers, and current power mode.
- **FR-020**: System MUST support Trust-On-First-Use (TOFU) key pinning with two modes: Strict (reject key changes) and Prompt (delegate to app callback).
- **FR-021**: System MUST support cut-through relay forwarding, enabling intermediate nodes to begin forwarding chunks to the next hop before all inbound chunks arrive.
- **FR-022**: System MUST enforce mesh isolation — peers with different application IDs never discover or connect to each other (FNV-1a hash in advertisement).
- **FR-023**: System MUST provide a DSL-based configuration builder with 8 sub-configurations, 4 presets, validation with safety-critical throws and best-effort clamping.
- **FR-024**: System MUST support regulatory region compliance (EU: ETSI EN 300 328 — advertisement interval ≥300 ms, scan duty cycle ≤70%).

### Key Entities

- **Peer**: A device running MeshLink identified by a 12-byte Key Hash derived from its Ed25519 + X25519 public keys.
- **Identity**: Two independent static keypairs (Ed25519 for signing, X25519 for key agreement) generated on first launch and stored in platform-secure storage.
- **Message**: An application payload (up to 100 KB) with a 128-bit random Message ID, priority level, and end-to-end encryption envelope.
- **Route**: A destination-to-next-hop mapping with cost metric, sequence number, feasibility distance, and expiry timestamp.
- **Transfer Session**: A stateful chunked transmission with SACK tracking, progress reporting, resume capability, and inactivity timeout.
- **Trust Store**: Persistent mapping of Peer Key Hash → pinned X25519 public key, supporting TOFU semantics.
- **Power Tier**: One of three fixed operational modes (Performance/Balanced/Power Saver) controlling radio duty cycle, connection budget, and chunk sizing.
- **Diagnostic Event**: A structured observation with code (26 defined), severity (4 levels), timestamp, and typed payload.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Two devices within BLE range discover each other and complete mutual authentication within 10 seconds of both starting the library.
- **SC-002**: End-to-end message delivery (single hop, 256 bytes) completes within 50 ms at p95 after connection establishment.
- **SC-003**: L2CAP data transfer sustains ≥80 KB/s on Android (Pixel 6+) and ≥60 KB/s on iOS (iPhone 12+) for single-hop connections.
- **SC-004**: Babel routing table converges within 3 seconds for a 10-node topology change.
- **SC-005**: Steady-state memory allocation does not exceed 8 MB with 8 connected peers and active routing.
- **SC-006**: In Power Saver mode, scan duty cycle does not exceed 5% and connection intervals are ≥500 ms.
- **SC-007**: Time from library start to first BLE advertisement is less than 500 ms on both platforms.
- **SC-008**: Messages routed through 3 hops arrive with zero data corruption and end-to-end encryption verified at each endpoint.
- **SC-009**: Pseudonym rotation occurs every 15 minutes and connected peers verify the new pseudonym with ≥99.9% success rate.
- **SC-010**: The library achieves 100% line and branch test coverage across all shared protocol logic.
- **SC-011**: All cryptographic operations pass validation against established test vectors (Wycheproof for X25519/Ed25519, RFC vectors for HKDF/ChaCha20-Poly1305).
- **SC-012**: The public API surface is identical in shape across Android and iOS, with no platform-specific types exposed to consumers.

## Assumptions

- Users have Android 10+ (API 29) or iOS 14+ devices with Bluetooth Low Energy hardware.
- BLE effective range is approximately 30–100 meters depending on environment and hardware.
- The library is consumed by third-party app developers as a dependency (not used standalone).
- No internet connectivity is available or required for any feature.
- Messages are ephemeral — the library does not provide long-term persistence beyond store-and-forward buffering.
- The consuming application handles its own UI for trust decisions (key changes in Prompt mode), data display, and user identity management.
- A maximum mesh size of approximately 50 peers is the practical limit for BLE mesh networking given radio constraints.
- The primary use case is proximity-based social discovery ("Tinder without Internet") where users broadcast profiles (≤10 KB) and open 1:1 channels after discovery.
