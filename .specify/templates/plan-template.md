# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: `specs/[###-feature-name]/spec.md`  
**Input**: Feature specification from `specs/[###-feature-name]/spec.md`  
**Memory Inputs**: `docs/memory/*`, `specs/[###-feature-name]/memory.md`, `specs/[###-feature-name]/memory-synthesis.md`

## Summary

[Primary requirement + technical approach. One paragraph.]

## Technical Context

**Language/Version**: Kotlin 2.3.20 (Kotlin Multiplatform)  
**Targets**: commonMain (shared), androidMain (API 29+), iosMain (arm64, iOS 15+), jvmMain (test/bench infra)  
**Build System**: Gradle 9.4.1+ (Kotlin DSL, Version Catalog)  
**Primary Dependencies**: kotlinx-coroutines 1.10.2, libsodium (Android JNI / iOS cinterop)  
**Testing**: kotlin.test + Power-assert, kotlinx-coroutines-test, Wycheproof vectors, MeshTestHarness  
**Coverage**: Kover 0.9.8 — 100% line + branch enforced  
**Static Analysis**: Detekt 1.23.8, ktfmt 0.26.0  
**API Tracking**: BCV 0.18.1 (JVM .api + iOS KLib)  
**Benchmarks**: kotlinx-benchmark 0.4.16 (JVM/JMH)  
**Publishing**: Maven Central (OSSRH) + SPM XCFramework  
**Performance Goals**: [Feature-specific — reference spec Performance Budget section]  
**Constraints**: No server dependency, single runtime dep (coroutines), wire backward compat

## Constitution Check

*GATE: Must pass before implementation. Re-check after design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ☐ | explicitApi(), Detekt clean, ktfmt formatted, BCV tracked |
| II. Testing | ☐ | 100% coverage achievable? Wycheproof vectors needed? Benchmark added? |
| III. UX Consistency | ☐ | API identical across targets? SKIE generates correct Swift? |
| IV. Performance | ☐ | Budget declared in spec? Benchmark validates? <10% regression? |
| Conventional Commits | ☐ | Human and automated commits for this feature use Conventional Commit messages |
| No server dependency | ☐ | Feature works fully offline? |
| Wire stability | ☐ | New wire messages backward-compatible? Schema updated? |
| Single runtime dep | ☐ | No new runtime dependencies added? |

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature-name]/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Phase 0 output (if applicable)
├── tasks.md             # Phase 2 output (/speckit.tasks)
├── memory.md            # Active feature memory
└── memory-synthesis.md  # Compact planning/implementation memory view
```

### Source Code Layout

```text
meshlink/src/
├── commonMain/kotlin/ch/trancee/meshlink/
│   ├── api/                 # Public API surface (MeshLinkApi, config, states)
│   ├── crypto/              # CryptoProvider, identity, trust store
│   │   └── noise/           # Noise XX handshake, CipherState, SymmetricState
│   ├── engine/              # MeshEngine orchestrator, state machine
│   ├── messaging/           # Delivery pipeline, cut-through relay
│   ├── power/               # Power tiers, connection limiter, drain
│   ├── routing/             # Babel routing engine, routing table
│   ├── storage/             # SecureStorage interface
│   ├── testing/             # Public test utilities (VirtualMeshTransport)
│   ├── transfer/            # SACK-based chunked transfer
│   ├── transport/           # BLE abstraction, L2CAP codec, advertisements
│   ├── util/                # Hex, LRU, jitter, ByteArrayKey
│   └── wire/                # Binary codec, message types
│       └── messages/        # Individual message codecs
├── commonTest/kotlin/ch/trancee/meshlink/
│   ├── [subsystem]/         # Unit tests mirror commonMain
│   └── integration/         # Multi-node integration tests (MeshTestHarness)
├── androidMain/kotlin/ch/trancee/meshlink/
│   ├── api/                 # MeshLinkAndroidFactory
│   ├── crypto/              # AndroidCryptoProvider (libsodium JNI)
│   ├── storage/             # AndroidSecureStorage (DataStore)
│   └── transport/           # AndroidBleTransport, MeshLinkService
├── androidHostTest/             # Android host-side tests (filtered; JVM shim pending)
├── iosMain/kotlin/ch/trancee/meshlink/
│   ├── api/                 # MeshLinkIosFactory
│   ├── crypto/              # IosCryptoProvider (libsodium cinterop)
│   ├── engine/              # MeshNode (iOS-specific helper)
│   ├── storage/             # IosSecureStorage
│   └── transport/           # IosBleTransport (CoreBluetooth)
├── iosTest/                     # iOS-specific tests (runs on macOS only)
└── jvmMain/kotlin/ch/trancee/meshlink/
    ├── benchmark/           # JMH benchmarks
    └── crypto/              # JvmCryptoProvider (JDK shim for tests)
```

### Files This Feature Touches

<!--
  List concrete files that will be created or modified.
  This enables conflict detection across parallel features.
-->

| Action | File | Reason |
|--------|------|--------|
| Create | `meshlink/src/commonMain/.../[file].kt` | [why] |
| Create | `meshlink/src/commonTest/.../[file]Test.kt` | [tests for above] |
| Modify | `meshlink/src/commonMain/.../MeshEngine.kt` | [wire new subsystem] |
| Modify | `meshlink/api/jvm/meshlink.api` | [BCV dump if public API changes] |

## Design Decisions

<!--
  Document key choices with rationale. Reference spec/ documents where applicable.
-->

| Decision | Choice | Rationale | Alternatives Rejected |
|----------|--------|-----------|----------------------|
| [e.g., Storage backend] | [chosen approach] | [why] | [what else was considered] |

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| [e.g., Coverage gap from coroutine state machine] | [Low/Med/High] | [Low/Med/High] | [e.g., Kover class exclude with justification] |

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., Kover class exclude] | [specific phantom branch] | [covering it introduces more phantoms] |
