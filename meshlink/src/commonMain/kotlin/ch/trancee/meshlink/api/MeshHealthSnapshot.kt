package ch.trancee.meshlink.api

import ch.trancee.meshlink.power.PowerTier

/**
 * Read-only operational snapshot that summarizes the runtime's health at a point in time.
 *
 * The snapshot is intentionally immutable so UIs, support tooling, and host applications can cache
 * or log it without worrying about concurrent runtime mutation.
 */
public class MeshHealthSnapshot(
  connectedPeers: List<PeerDetail>,
  /** Number of destinations currently represented in the routing table. */
  public val routingTableSize: Int,
  /** Number of active transfer sessions currently tracked by the engine. */
  public val activeTransferCount: Int,
  /** Number of application payloads currently buffered for deferred delivery. */
  public val bufferedMessageCount: Int,
  /** Power-management tier currently governing scan, connection, and transfer behavior. */
  public val powerTier: PowerTier,
) {
  init {
    require(routingTableSize >= 0) {
      "MeshHealthSnapshot routingTableSize must be greater than or equal to 0."
    }
    require(activeTransferCount >= 0) {
      "MeshHealthSnapshot activeTransferCount must be greater than or equal to 0."
    }
    require(bufferedMessageCount >= 0) {
      "MeshHealthSnapshot bufferedMessageCount must be greater than or equal to 0."
    }
  }

  /** Immutable copy of the peers considered transport-connected at snapshot time. */
  public val connectedPeers: List<PeerDetail> = connectedPeers.toList()

  /** Convenience count derived from [connectedPeers] for dashboard-style health displays. */
  public val connectedPeerCount: Int
    get() = connectedPeers.size

  /** Returns a compact summary that is suitable for logs and diagnostics. */
  override fun toString(): String {
    return "MeshHealthSnapshot(connectedPeerCount=$connectedPeerCount, routingTableSize=$routingTableSize, activeTransferCount=$activeTransferCount, bufferedMessageCount=$bufferedMessageCount, powerTier=$powerTier)"
  }
}
