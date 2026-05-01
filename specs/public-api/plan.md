# Implementation Plan: Public API & Diagnostics

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/public-api/spec.md`  
**Status**: Migrated — implementation complete

## Summary

Public-facing API surface for MeshLink library consumers. Includes the main MeshLinkApi interface, configuration DSL, lifecycle state machine, diagnostic event system, and platform factory functions.

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/api/
├── DiagnosticCode.kt          # 26 event codes (enum with severity)
├── DiagnosticPayload.kt       # Typed payload sealed hierarchy
├── DiagnosticSink.kt          # SharedFlow ring buffer with drop counting
├── ExperimentalMeshLinkApi.kt # Opt-in annotation
├── MeshLink.kt                # Factory companion + Flow mapping
├── MeshLinkApi.kt             # Main public interface
├── MeshLinkConfig.kt          # Configuration builder DSL
├── MeshLinkState.kt           # Lifecycle state machine (6 states)
├── NoOpDiagnosticSink.kt      # Zero-overhead opt-out
├── PeerDetail.kt              # Peer information snapshot
├── PeerIdHex.kt               # Type-safe peer identifier
├── PeerState.kt               # Peer lifecycle sealed class
├── RegulatoryRegion.kt        # Radio parameter clamping enum
├── RoutingSnapshot.kt         # Routing table debug snapshot
└── TrustMode.kt               # TOFU/STRICT/PROMPT enum
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| API style | Interface + companion factory | Testable, platform-specific factory functions |
| Config | Builder DSL with validation | Type-safe, discoverable, clamped to legal ranges |
| Diagnostics | SharedFlow ring buffer | Non-blocking, bounded memory, backpressure-free |
| State machine | Enum + internal transition function | Exhaustive, simple, BCV-tracked |
| Platform factories | `MeshLink.createAndroid()`, `MeshLink.createIos()` | Injects platform deps (Context, etc.) |
