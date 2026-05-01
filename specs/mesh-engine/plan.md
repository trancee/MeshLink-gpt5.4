# Implementation Plan: Mesh Engine

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/mesh-engine/spec.md`  
**Status**: Migrated — implementation complete

## Summary

Central orchestrator that wires crypto, routing, messaging, transfer, transport, and power subsystems. Manages lifecycle, handshakes, pseudonym rotation, and periodic state sweeps.

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/engine/
├── HandshakeConfig.kt          # Handshake timeout, retry settings
├── MeshEngine.kt               # Core orchestrator (~1570 lines)
├── MeshEngineConfig.kt         # Aggregate config for all subsystems
├── MeshStateManager.kt         # Periodic sweep of stale state
├── NoiseHandshakeManager.kt    # Noise XX handshake orchestration
├── PowerTierCodec.kt           # Power tier ↔ advertisement byte encoding
└── PseudonymRotator.kt         # HMAC-based epoch pseudonym rotation
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Wiring | Constructor injection in `MeshEngine.create()` | No DI framework; explicit, testable |
| Lifecycle | State machine (MeshLinkState enum) | Clear transitions, exhaustive handling |
| Pseudonyms | HMAC-SHA-256 + epoch + stagger | Deterministic, prevents tracking, no coordination needed |
| Sweeps | MeshStateManager with configurable interval | Centralized cleanup logic |
| Handshakes | NoiseHandshakeManager per-peer | Isolates handshake state from engine |
