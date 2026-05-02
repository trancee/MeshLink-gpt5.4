package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex

/** Deterministic tie-breaker for deciding which side initiates a connection. */
public object ConnectionInitiationPolicy {
  /**
   * Returns true when the local peer should initiate the connection.
   *
   * Lexicographic ordering ensures both peers make the same decision independently.
   */
  public fun shouldLocalPeerInitiate(localPeerId: PeerIdHex, remotePeerId: PeerIdHex): Boolean {
    val normalizedLocalPeerId: String = localPeerId.value.lowercase()
    val normalizedRemotePeerId: String = remotePeerId.value.lowercase()

    if (normalizedLocalPeerId == normalizedRemotePeerId) {
      return false
    }

    return normalizedLocalPeerId < normalizedRemotePeerId
  }

  /** Chooses the preferred data path before connection establishment begins. */
  internal fun preferredDataPath(
    preferL2cap: Boolean,
    cachedCapability: OemL2capProbeResult?,
  ): TransportDataPath {
    if (!preferL2cap) {
      return TransportDataPath.GATT
    }
    if (cachedCapability == null) {
      return TransportDataPath.L2CAP
    }
    if (cachedCapability.supportsL2cap) {
      return TransportDataPath.L2CAP
    }
    return if (cachedCapability.isStale) {
      TransportDataPath.L2CAP
    } else {
      TransportDataPath.GATT
    }
  }

  /** Demotes a failed transport attempt to the bounded fallback path. */
  internal fun fallbackDataPath(failedDataPath: TransportDataPath): TransportDataPath {
    return when (failedDataPath) {
      TransportDataPath.L2CAP,
      TransportDataPath.GATT -> TransportDataPath.GATT
    }
  }
}

internal enum class TransportDataPath {
  L2CAP,
  GATT,
}
