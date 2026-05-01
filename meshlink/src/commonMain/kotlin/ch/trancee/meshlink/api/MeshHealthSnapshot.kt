package ch.trancee.meshlink.api

import ch.trancee.meshlink.power.PowerTier

/** Read-only operational snapshot that summarizes the runtime's health at a point in time. */
public class MeshHealthSnapshot(
  connectedPeers: List<PeerDetail>,
  public val routingTableSize: Int,
  public val activeTransferCount: Int,
  public val bufferedMessageCount: Int,
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

  public val connectedPeers: List<PeerDetail> = connectedPeers.toList()

  public val connectedPeerCount: Int
    get() = connectedPeers.size

  override fun toString(): String {
    return "MeshHealthSnapshot(connectedPeerCount=$connectedPeerCount, routingTableSize=$routingTableSize, activeTransferCount=$activeTransferCount, bufferedMessageCount=$bufferedMessageCount, powerTier=$powerTier)"
  }
}
