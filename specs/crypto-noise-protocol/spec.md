# Feature Specification: Crypto & Noise Protocol

**Status**: Migrated  
**Spec References**: spec/05-security-encryption.md, spec/06-security-identity-trust.md  
**Subsystem**: `crypto/`, `crypto/noise/`

## User Scenarios & Testing

### User Story 1 - Noise XX Mutual Authentication (Priority: P1)

Two peers meeting for the first time perform a Noise XX handshake to mutually authenticate
and establish a shared transport encryption session.

**Independent Test**: `NoiseXXHandshakeTest` — full 3-message handshake between initiator/responder

**Acceptance Scenarios**:

1. **Given** two peers with fresh identities, **When** they complete a 3-message Noise XX exchange, **Then** both derive identical CipherState pairs for encrypt/decrypt
2. **Given** a handshake in progress, **When** the remote static key is revealed (message 3), **Then** the TrustStore is consulted for acceptance/rejection
3. **Given** a peer with a known static key (TOFU), **When** they reconnect, **Then** the handshake succeeds if the key matches the pinned value

### User Story 2 - Noise K Payload Encryption (Priority: P1)

After handshake, all application payloads are sealed/opened using Noise K (ChaCha20-Poly1305 AEAD).

**Independent Test**: `NoiseKSealOpenTest` — round-trip encrypt/decrypt with nonce advancement

**Acceptance Scenarios**:

1. **Given** an established session with CipherState, **When** a payload is sealed, **Then** it produces ciphertext + 16-byte Poly1305 tag
2. **Given** a sealed payload, **When** opened with the correct CipherState, **Then** the original plaintext is recovered
3. **Given** a tampered ciphertext, **When** open is attempted, **Then** decryption fails (tag mismatch)

### User Story 3 - Replay Protection (Priority: P1)

A sliding-window replay guard prevents replayed or reordered packets from being accepted.

**Independent Test**: `ReplayGuardTest` — window advancement, duplicate rejection, out-of-order acceptance

**Acceptance Scenarios**:

1. **Given** a nonce already seen, **When** check is called, **Then** it returns false (duplicate)
2. **Given** a nonce within the sliding window but not yet seen, **When** check is called, **Then** it returns true and marks as seen
3. **Given** a nonce far ahead of the window, **When** check is called, **Then** the window advances and old entries are discarded

### User Story 4 - Identity & Trust Management (Priority: P2)

Peers have Ed25519 identity keys. TrustStore manages key pinning (TOFU/STRICT/PROMPT modes).

**Independent Test**: `IdentityTest`, `TrustStoreTest` — key generation, pinning, conflict detection

**Acceptance Scenarios**:

1. **Given** TrustMode.TOFU, **When** a new peer presents a key, **Then** it is pinned on first contact
2. **Given** a pinned key in STRICT mode, **When** a different key is presented for the same peer, **Then** the connection is rejected
3. **Given** an identity rotation announcement, **When** the old key signs the new key, **Then** the pin is updated

### Edge Cases

- Constant-time comparison for all secret-dependent operations (`ConstantTimeEquals`)
- DH computation caching to avoid redundant X25519 operations (`DhCache`)
- CipherState nonce overflow detection (must rekey before 2^64)

## Requirements

- **FR-001**: CryptoProvider interface MUST abstract all primitives (X25519, Ed25519, ChaCha20-Poly1305, HKDF, HMAC-SHA256)
- **FR-002**: Noise XX handshake MUST follow the Noise Protocol Framework spec (3 messages, XX pattern)
- **FR-003**: Noise K sealing MUST use AEAD with 96-bit nonce derived from CipherState counter
- **FR-004**: ReplayGuard MUST use a 64-entry sliding bitmap window
- **FR-005**: TrustStore MUST support TOFU, STRICT, and PROMPT modes
- **FR-006**: Identity keys MUST be Ed25519 (32-byte public, 64-byte secret)
- **FR-007**: All crypto MUST be validated against Wycheproof test vectors

## Wire Protocol Impact

- `MessageType.HANDSHAKE` carries Noise XX messages (3 rounds)
- `MessageType.ROTATION_ANNOUNCEMENT` carries signed key rotation proof

## Security Analysis

- **Threat model**: MITM, replay attacks, key compromise, identity spoofing
- **Crypto primitives**: X25519 DH, ChaCha20-Poly1305 AEAD, Ed25519 signing, HKDF, HMAC-SHA256
- **Key material**: Ephemeral keys zeroed after handshake; static keys in SecureStorage
- **Wycheproof coverage**: AEAD vectors, Ed25519 vectors, X25519 vectors, HKDF vectors

## Performance Budget

| Metric | Budget | Measurement |
|--------|--------|-------------|
| Handshake latency | <5ms (3 messages combined) | Unit test timing |
| AEAD seal/open | <1μs per operation | WireFormatBenchmark |

## Success Criteria

- **SC-001**: All Wycheproof test vectors pass (ChaCha20-Poly1305, Ed25519, X25519, HKDF)
- **SC-002**: Noise XX handshake produces matching CipherState pairs
- **SC-003**: Replay guard rejects duplicates within and beyond window
- **SC-004**: TrustStore correctly enforces TOFU/STRICT/PROMPT semantics
- **SC-005**: 100% line and branch coverage (koverVerify green)
