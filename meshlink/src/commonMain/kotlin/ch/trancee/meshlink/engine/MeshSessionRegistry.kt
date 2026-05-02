package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.TrustDecision

/**
 * Tracks per-peer runtime coordination state that spans transport, trust, routing, and transfer
 * concerns.
 */
internal class MeshSessionRegistry {
  private val sessionsByPeer: MutableMap<String, MeshSessionRecord> = linkedMapOf()

  internal fun upsert(
    peerId: PeerIdHex,
    transportConnected: Boolean,
    trustDecision: TrustDecision?,
    routeAvailable: Boolean,
    activeTransferIds: Set<String>,
  ): MeshSessionRecord {
    val record =
      MeshSessionRecord(
        peerId = peerId,
        transportConnected = transportConnected,
        trustDecision = trustDecision,
        routeAvailable = routeAvailable,
        activeTransferIds = activeTransferIds,
      )
    sessionsByPeer[peerId.value] = record
    return record
  }

  internal fun session(peerId: PeerIdHex): MeshSessionRecord? {
    return sessionsByPeer[peerId.value]
  }

  internal fun remove(peerId: PeerIdHex): MeshSessionRecord? {
    return sessionsByPeer.remove(peerId.value)
  }

  internal fun connectedPeerIds(): List<PeerIdHex> {
    return sessionsByPeer.values
      .filter { record -> record.transportConnected }
      .map { record -> record.peerId }
  }

  internal fun activeTransferCount(): Int {
    return sessionsByPeer.values.sumOf { record -> record.activeTransferIds.size }
  }

  internal fun snapshot(): List<MeshSessionRecord> {
    return sessionsByPeer.values.toList()
  }

  internal fun clear(): Unit {
    sessionsByPeer.clear()
  }
}

internal class MeshSessionRecord(
  internal val peerId: PeerIdHex,
  internal val transportConnected: Boolean,
  internal val trustDecision: TrustDecision?,
  internal val routeAvailable: Boolean,
  activeTransferIds: Set<String>,
) {
  internal val activeTransferIds: Set<String> = activeTransferIds.toSet()

  internal val isTrusted: Boolean
    get() = trustDecision == TrustDecision.Accepted || trustDecision == TrustDecision.Pinned
}
