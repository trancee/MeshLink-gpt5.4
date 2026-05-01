# Spec Drift Report

Generated: 2026-04-30T19:31:07+00:00
Project: MeshLink

## Summary

| Category | Count |
|----------|-------|
| Specs Analyzed | 11 |
| Requirements Checked | 89 |
| ✓ Aligned | 0 (0%) |
| ⚠️ Drifted | 0 (0%) |
| ✗ Not Implemented | 89 (100%) |
| 🆕 Unspecced Code | 0 |

## Global Observations

- No product implementation tree was found. The repository currently contains specifications, RFC references, and Spec Kit / agent infrastructure, but no `meshlink/`, `src/`, `app/`, or `lib/` source files for the MeshLink product itself.
- No build or delivery artifacts were found for the specified product implementation: no `gradlew`, `settings.gradle.kts`, `build.gradle.kts`, or project-owned `.github/workflows/*.yml` files exist at the repository root.
- No product test files or benchmark files were found, so none of the acceptance scenarios or success criteria can be validated against executable code.
- Tooling and governance content under `.specify/`, `.pi/`, `.agents/`, and `docs/rfcs/` was treated as repository infrastructure and excluded from “unspecced product code” findings.

## Detailed Findings

### Spec: 001-codebase-spec-analysis - MeshLink — Encrypted Serverless BLE Mesh SDK

Spec inventory: 24 functional requirements, 12 success criteria, 38 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- None detected beyond the absence of implementation; all requirement-level gaps are classified as not implemented.

#### Not Implemented ✗
- FR-001: System MUST discover peers within BLE range that share the same application identifier, without requiring internet connectivity, servers, or user accounts.
- FR-002: System MUST perform mutual cryptographic authentication (Noise XX handshake) with every discovered peer before any application data is exchanged.
- FR-003: System MUST encrypt all unicast messages end-to-end using authenticated encryption (Noise K with ChaCha20-Poly1305), ensuring only the intended recipient can decrypt.
- FR-004: System MUST route messages through intermediate peers when the sender and recipient are not within direct BLE range (multi-hop mesh routing).
- FR-005: System MUST converge routing tables within 3 seconds for topology changes up to 10 nodes (Babel-based distance-vector routing).
- FR-006: System MUST provide store-and-forward buffering for messages destined to temporarily unreachable peers, with configurable buffer capacity and priority-based eviction.
- FR-007: System MUST support broadcast messaging with Ed25519 signatures, configurable hop radius, and per-message deduplication.
- FR-008: System MUST support chunked transfer of large payloads (up to 100 KB) with selective acknowledgement (SACK), progress tracking, and byte-offset resume after disconnection.
- FR-009: System MUST provide three battery-adaptive power tiers (Performance, Balanced, Power Saver) that automatically adjust scan duty cycle, connection limits, and chunk sizes based on battery level.
- FR-010: System MUST expose a reactive event stream for peer discovery, message receipt, delivery confirmations, transfer progress, transfer failures, key changes, and diagnostic events.
- FR-011: System MUST protect user privacy by rotating BLE advertising pseudonyms every 15 minutes using HMAC-derived identifiers that connected peers can verify.
- FR-012: System MUST prevent message replay attacks using a 64-bit sliding window per sender.
- FR-013: System MUST support identity key rotation with signed announcements propagated to the mesh, invalidating existing sessions and requiring new handshakes.
- FR-014: System MUST support GDPR data erasure: per-peer forget (erases all local state) and full factory reset.
- FR-015: System MUST support lifecycle management (start, stop, pause, resume) with a deterministic state machine (UNINITIALIZED → RUNNING → PAUSED → STOPPED, plus RECOVERABLE and TERMINAL error states).
- FR-016: System MUST prefer L2CAP transport for data transfer (3–10× throughput over GATT) with automatic fallback to GATT when L2CAP is unavailable.
- FR-017: System MUST provide an identical API surface on Android and iOS with platform differences confined to internal implementations.
- FR-018: System MUST support configurable rate limiting for outbound unicast messages, broadcasts, and handshake initiations.
- FR-019: System MUST provide health snapshots including connected peers, routing table size, buffer usage, active transfers, and current power mode.
- FR-020: System MUST support Trust-On-First-Use (TOFU) key pinning with two modes: Strict (reject key changes) and Prompt (delegate to app callback).
- FR-021: System MUST support cut-through relay forwarding, enabling intermediate nodes to begin forwarding chunks to the next hop before all inbound chunks arrive.
- FR-022: System MUST enforce mesh isolation — peers with different application IDs never discover or connect to each other (FNV-1a hash in advertisement).
- FR-023: System MUST provide a DSL-based configuration builder with 8 sub-configurations, 4 presets, validation with safety-critical throws and best-effort clamping.
- FR-024: System MUST support regulatory region compliance (EU: ETSI EN 300 328 — advertisement interval ≥300 ms, scan duty cycle ≤70%).

### Spec: babel-routing - Babel Routing

Spec inventory: 6 functional requirements, 5 success criteria, 5 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: RoutingEngine MUST implement Babel distance-vector with feasibility conditions
- FR-002: RoutingTable MUST support multiple routes per destination with metric comparison
- FR-003: RouteCoordinator MUST handle seqno requests for starvation recovery
- FR-004: PresenceTracker MUST emit peer appeared/disappeared events with configurable timeout
- FR-005: DedupSet MUST deduplicate with bounded memory (LRU eviction)
- FR-006: Routing convergence MUST complete within 3 seconds for 10-node topology changes

### Spec: ble-transport - BLE Transport

Spec inventory: 7 functional requirements, 5 success criteria, 4 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: BleTransport interface MUST abstract L2CAP and GATT behind a common API
- FR-002: L2capFrameCodec MUST handle frame encoding with length prefix
- FR-003: L2capRetryScheduler MUST implement exponential backoff with jitter
- FR-004: AdvertisementCodec MUST encode pseudonym + power tier in ≤31 bytes
- FR-005: ConnectionInitiationPolicy MUST determine which peer initiates (deterministic from peer IDs)
- FR-006: OemL2capProbeCache MUST cache L2CAP capability per device model
- FR-007: GattConstants MUST define service/characteristic UUIDs

### Spec: crypto-noise-protocol - Crypto & Noise Protocol

Spec inventory: 7 functional requirements, 5 success criteria, 12 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: CryptoProvider interface MUST abstract all primitives (X25519, Ed25519, ChaCha20-Poly1305, HKDF, HMAC-SHA256)
- FR-002: Noise XX handshake MUST follow the Noise Protocol Framework spec (3 messages, XX pattern)
- FR-003: Noise K sealing MUST use AEAD with 96-bit nonce derived from CipherState counter
- FR-004: ReplayGuard MUST use a 64-entry sliding bitmap window
- FR-005: TrustStore MUST support TOFU, STRICT, and PROMPT modes
- FR-006: Identity keys MUST be Ed25519 (32-byte public, 64-byte secret)
- FR-007: All crypto MUST be validated against Wycheproof test vectors

### Spec: mesh-engine - Mesh Engine

Spec inventory: 7 functional requirements, 5 success criteria, 6 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: MeshEngine MUST wire all subsystems via constructor injection (no DI framework)
- FR-002: NoiseHandshakeManager MUST orchestrate 3-message XX flow per peer
- FR-003: PseudonymRotator MUST compute HMAC-SHA-256 pseudonyms on configurable epoch boundaries
- FR-004: Pseudonym stagger MUST be deterministic per-node (prevent simultaneous rotation)
- FR-005: MeshStateManager MUST sweep stale peers, expired routes, and unhealthy connections
- FR-006: PowerTierCodec MUST encode/decode power tier in advertisement payload
- FR-007: Engine MUST emit diagnostics for all handshake events and state transitions

### Spec: messaging-delivery - Messaging & Delivery Pipeline

Spec inventory: 5 functional requirements, 4 success criteria, 5 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: DeliveryPipeline MUST guarantee at-most-once delivery semantics
- FR-002: Cut-through relay MUST forward chunk0 within 1 hop without full buffering
- FR-003: SlidingWindowRateLimiter MUST enforce configurable per-peer-pair limits
- FR-004: Message ID MUST be unique per sender (monotonic counter)
- FR-005: DeliveryPipeline MUST emit diagnostics for timeout, buffer pressure, cancellation

### Spec: platform-distribution - Platform & Distribution

Spec inventory: 9 functional requirements, 5 success criteria, 9 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: Android artifact MUST be published as AAR to Maven Central
- FR-002: iOS MUST be distributed as static XCFramework via SPM binary target
- FR-003: BCV MUST track JVM .api file + iOS KLib ABI (macOS only)
- FR-004: SKIE MUST generate Swift-friendly wrappers (exhaustive enums, AsyncStream)
- FR-005: CI MUST run: lint → test → coverage → apiCheck → benchmark (ci.yml)
- FR-006: Release MUST run: publish-android → publish-ios → publish-xcframework (release.yml)
- FR-007: CodeQL MUST scan weekly (actions, c-cpp, java-kotlin)
- FR-008: Signing MUST use in-memory PGP keys from CI secrets
- FR-009: ProGuard consumer rules MUST be shipped alongside AAR

### Spec: power-management - Power Management

Spec inventory: 6 functional requirements, 4 success criteria, 7 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: PowerManager MUST evaluate battery level and select appropriate PowerTier
- FR-002: PowerProfile MUST define scan intervals, connection limits, and duty cycles per tier
- FR-003: ConnectionLimiter MUST enforce max concurrent connections per tier
- FR-004: GracefulDrainManager MUST allow in-flight transfers to complete before disconnect
- FR-005: BleConnectionParameterPolicy MUST adjust BLE connection parameters per tier
- FR-006: In LOW tier, scan duty cycle MUST NOT exceed 5%

### Spec: public-api - Public API & Diagnostics

Spec inventory: 8 functional requirements, 5 success criteria, 6 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: MeshLinkApi MUST expose: start, stop, pause, resume, send, peers, messages, diagnosticEvents
- FR-002: MeshLinkConfig DSL MUST validate all inputs and clamp to legal ranges
- FR-003: DiagnosticSink MUST use SharedFlow ring buffer with DROP_OLDEST
- FR-004: 26 DiagnosticCode values MUST cover all subsystem events
- FR-005: PeerState sealed hierarchy MUST represent all peer lifecycle states
- FR-006: RegulatoryRegion MUST clamp radio parameters to legal limits
- FR-007: TrustMode enum MUST expose TOFU, STRICT, PROMPT
- FR-008: API shape MUST be identical across Android, iOS, JVM targets (SKIE for Swift)

### Spec: sack-transfer - SACK Transfer

Spec inventory: 5 functional requirements, 5 success criteria, 3 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: TransferEngine MUST split payloads into chunks respecting ChunkSizePolicy
- FR-002: SackTracker MUST track selective acknowledgments per transfer
- FR-003: ResumeCalculator MUST compute correct resume offset from SACK state
- FR-004: TransferScheduler MUST order transfers by Priority (HIGH > NORMAL > LOW)
- FR-005: ObservationRateController MUST adapt send rate to observed throughput

### Spec: wire-format - Wire Format & Codec

Spec inventory: 5 functional requirements, 4 success criteria, 6 acceptance scenarios.

#### Aligned ✓
- None. No product implementation files were found for this spec.

#### Drifted ⚠️
- Supporting plan/tasks artifacts exist and in several cases claim migrated/complete status, but no corresponding implementation files are present in the repository.

#### Not Implemented ✗
- FR-001: WireCodec MUST support all 11+ message types (Hello, Handshake, Update, Chunk, ChunkAck, DeliveryAck, Nack, Keepalive, Broadcast, RoutedMessage, ResumeRequest, RotationAnnouncement)
- FR-002: Encoding MUST be deterministic (same input → same bytes)
- FR-003: MessageType tag MUST be 1 byte (compact for BLE MTU)
- FR-004: InboundValidator MUST reject messages exceeding configured bounds
- FR-005: ReadBuffer/WriteBuffer MUST handle little-endian encoding without allocations in hot path

## Unspecced Code 🆕

No product implementation code was detected outside the specifications, so there are no unspecced product features to report.

## Inter-Spec Conflicts

- Requirement overlap: `001-codebase-spec-analysis` is an umbrella spec that duplicates capabilities split across the subsystem specs, so future updates can easily diverge unless one source is declared canonical.

## Recommendations

1. Decide whether this repository is intended to remain spec-only. If not, bootstrap the actual MeshLink implementation tree (`meshlink/` or equivalent), build files, and CI workflows so requirement-level drift can be measured against code rather than absence.
2. Keep `001-codebase-spec-analysis` as an umbrella overview and treat subsystem specs as canonical for detailed normative requirements.
3. After code is added, re-run `/speckit.sync.analyze` and then use `/speckit.sync.backfill` or `/speckit.sync.propose` only for genuinely unspecced implementation, not for the current repository infrastructure.
