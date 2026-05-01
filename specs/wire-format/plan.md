# Implementation Plan: Wire Format & Codec

**Branch**: main (migrated) | **Date**: 2026-04-30 | **Spec**: `specs/wire-format/spec.md`  
**Status**: Migrated — implementation complete

## Summary

Custom pure-Kotlin binary codec for all mesh protocol messages. Zero external dependencies at runtime. Designed for BLE MTU constraints (~512 bytes typical L2CAP, ~247 GATT).

## Project Structure

```text
meshlink/src/commonMain/kotlin/ch/trancee/meshlink/wire/
├── InboundValidator.kt        # Length/field/hop-limit validation
├── MessageType.kt             # 1-byte enum tag for all messages
├── ReadBuffer.kt              # Little-endian binary reader
├── ValidationResult.kt        # Valid/Invalid result type
├── WireCodec.kt               # Encode/decode dispatcher
├── WriteBuffer.kt             # Little-endian binary writer
└── messages/
    ├── Broadcast.kt           # Flood-fill broadcast payload
    ├── Chunk.kt               # Transfer chunk (SACK)
    ├── ChunkAck.kt            # Selective ACK for chunks
    ├── DeliveryAck.kt         # End-to-end delivery confirmation
    ├── Handshake.kt           # Noise XX handshake message
    ├── Hello.kt               # Peer discovery hello
    ├── Keepalive.kt           # Link keepalive
    ├── Nack.kt                # Negative acknowledgment
    ├── ResumeRequest.kt       # Transfer resume after disconnect
    ├── RotationAnnouncementMessage.kt  # Key rotation proof
    ├── RoutedMessage.kt       # Multi-hop routed payload
    └── Update.kt              # Babel routing update
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Codec library | Pure Kotlin (no FlatBuffers at runtime) | Zero runtime deps, full control over encoding |
| Endianness | Little-endian | Matches ARM native order (Android/iOS) |
| Message tag | 1-byte enum | Compact for BLE; 256 message types sufficient |
| Validation | Separate InboundValidator | Decouples parsing from policy enforcement |
| FlatBuffers | JVM benchmarks only | Schema in spec/schemas/meshlink.fbs for documentation |
