# Implementation Plan: Crypto & Noise Protocol

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/crypto-noise-protocol/spec.md`  
**Status**: Migrated — implementation complete

## Summary

Full cryptographic subsystem implementing Noise Protocol Framework (XX handshake + K payload encryption), identity management via Ed25519, trust pinning (TOFU/STRICT/PROMPT), and replay protection via sliding-window bitmap.

## Technical Context

**Language/Version**: Kotlin 2.3.20 (KMP commonMain)  
**Crypto Backend**: libsodium via platform actuals (Android JNI, iOS cinterop, JVM JDK shim)  
**Validation**: Wycheproof test vectors (AEAD, Ed25519, X25519, HKDF)  
**Spec References**: spec/05-security-encryption.md, spec/06-security-identity-trust.md

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/
├── crypto/
│   ├── ConstantTimeEquals.kt      # Timing-safe byte comparison
│   ├── CryptoProvider.kt          # Platform-abstract crypto interface
│   ├── CryptoProviderFactory.kt   # expect/actual factory
│   ├── Identity.kt                # Ed25519 keypair + keyHash
│   ├── KeyPair.kt                 # Generic key pair wrapper
│   ├── ReplayGuard.kt             # 64-entry sliding bitmap
│   ├── RotationAnnouncement.kt    # Signed key rotation proof
│   ├── TrustStore.kt              # TOFU/STRICT/PROMPT key pinning
│   └── noise/
│       ├── CipherState.kt         # ChaCha20-Poly1305 encrypt/decrypt state
│       ├── DhCache.kt             # Memoized X25519 computations
│       ├── EmptyByteArray.kt      # Sentinel for empty payloads
│       ├── HandshakeState.kt      # Noise XX state machine
│       ├── NoiseKOpen.kt          # K-pattern decryption
│       ├── NoiseKSeal.kt          # K-pattern encryption
│       ├── NoiseSession.kt        # Established session (send/recv CipherState)
│       ├── NoiseXXHandshake.kt    # 3-message XX pattern implementation
│       └── SymmetricState.kt      # Noise symmetric state (MixKey, MixHash)
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Crypto abstraction | `CryptoProvider` interface | Platform actuals (libsodium/JDK) behind common API |
| DH caching | `DhCache` with LRU eviction | Avoid redundant X25519 in handshake retries |
| Replay window | 64-bit bitmap | Sufficient for BLE MTU-limited traffic; O(1) check |
| Trust model | TOFU default, STRICT/PROMPT optional | Balance security with UX for mesh discovery |
| Nonce scheme | 64-bit counter per CipherState | Per Noise spec; overflow triggers rekey |

## Constitution Check

| Principle | Status |
|-----------|--------|
| I. Code Quality | ✅ explicitApi, Detekt clean, ktfmt formatted |
| II. Testing | ✅ 100% coverage, Wycheproof vectors, Power-assert |
| III. UX Consistency | ✅ CryptoProvider identical on all targets |
| IV. Performance | ✅ <1μs AEAD operations (benchmark validated) |
