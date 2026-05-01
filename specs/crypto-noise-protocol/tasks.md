# Tasks: Crypto & Noise Protocol

**Status**: In progress — foundation types and constant-time utility bootstrapped

## Phase 1: Foundation

- [x] T001 Define `CryptoProvider` interface (X25519, Ed25519, ChaCha20-Poly1305, HKDF, HMAC-SHA256)
- [x] T002 [P] Implement `CryptoProviderFactory` expect/actual pattern
- [x] T003 [P] Implement `ConstantTimeEquals` utility
- [x] T004 [P] Define `KeyPair` and `Identity` data classes
- [ ] T005 Write `CryptoProviderTest` validating all primitives

## Phase 2: Noise Protocol Core

- [x] T006 Implement `SymmetricState` (MixKey, MixHash, EncryptAndHash, DecryptAndHash)
- [x] T007 [P] Implement `CipherState` (InitializeKey, EncryptWithAd, DecryptWithAd, nonce tracking)
- [ ] T008 Implement `HandshakeState` (XX pattern state machine)
- [ ] T009 Implement `NoiseXXHandshake` (3-message initiator/responder flow)
- [x] T010 [P] Implement `DhCache` (LRU memoization of X25519 results)
- [x] T011 [P] Implement `NoiseSession` (established session with send/recv CipherState pair)
- [ ] T012 Write `NoiseXXHandshakeTest` — full handshake round-trip
- [x] T013 Write `SymmetricStateTest`, `CipherStateTest`, `DhCacheTest`

## Phase 3: Noise K Payload Encryption

- [x] T014 Implement `NoiseKSeal` (encrypt payload with sender CipherState)
- [x] T015 [P] Implement `NoiseKOpen` (decrypt payload with receiver CipherState)
- [x] T016 Write `NoiseKSealOpenTest` — round-trip seal/open with nonce advancement

## Phase 4: Replay Protection

- [x] T017 Implement `ReplayGuard` (64-entry sliding bitmap window)
- [x] T018 Write `ReplayGuardTest` — duplicates, out-of-order, window advance

## Phase 5: Identity & Trust

- [x] T019 Implement `Identity` (Ed25519 key generation, keyHash derivation)
- [x] T020 Implement `TrustStore` (TOFU/STRICT/PROMPT pinning, key conflict handling)
- [x] T021 [P] Implement `RotationAnnouncement` (signed proof of key rotation)
- [x] T022 Write `IdentityTest`, `TrustStoreTest`

## Phase 6: Wycheproof Validation

- [ ] T023 [P] Add `WycheproofAeadVectors` — ChaCha20-Poly1305 test vectors
- [ ] T024 [P] Add `WycheproofEd25519Vectors` — Ed25519 signing vectors
- [ ] T025 [P] Add `WycheproofX25519Vectors` — X25519 DH vectors
- [ ] T026 [P] Add `WycheproofHkdfVectors` — HKDF-SHA256 vectors
- [ ] T027 Write `WycheproofCryptoTest` — runner for all vector files
- [ ] T028 Write `HmacSha256Test` — HMAC correctness

## Phase 7: Platform Actuals

- [ ] T029 [P] Implement `AndroidCryptoProvider` (libsodium JNI via SodiumJni)
- [ ] T030 [P] Implement `IosCryptoProvider` (libsodium cinterop)
- [ ] T031 [P] Implement `JvmCryptoProvider` (JDK shim for test infrastructure)

## Verification

```bash
./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:detekt :meshlink:ktfmtCheck
```

18 actionable tasks complete, 13 remain pending. Current JVM and Android host-test coverage verify at 100%, and `:meshlink:jvmBenchmark` remains green.
