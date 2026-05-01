package ch.trancee.meshlink.power

public class GracefulDrainManager {
  public fun evaluate(connections: List<ManagedConnection>): GracefulDrainDecision {
    val inFlightConnections: List<ManagedConnection> =
      connections.filter { connection -> connection.transferStatus == TransferStatus.IN_FLIGHT }

    return if (inFlightConnections.isEmpty()) {
      GracefulDrainDecision(
        drainComplete = true,
        connectionsToClose = connections.map { connection -> connection.peerKey },
      )
    } else {
      GracefulDrainDecision(
        drainComplete = false,
        connectionsToClose =
          connections
            .filter { connection -> connection.transferStatus != TransferStatus.IN_FLIGHT }
            .map { connection -> connection.peerKey },
      )
    }
  }
}

public data class GracefulDrainDecision(
  public val drainComplete: Boolean,
  public val connectionsToClose: List<PeerKey>,
)
