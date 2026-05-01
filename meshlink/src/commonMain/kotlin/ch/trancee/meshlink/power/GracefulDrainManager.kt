package ch.trancee.meshlink.power

/**
 * Chooses which connections may be closed during a graceful drain.
 *
 * In-flight transfers are preserved until they finish, while idle connections may be closed
 * immediately.
 */
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

/** Result of evaluating graceful drain eligibility. */
public data class GracefulDrainDecision(
  public val drainComplete: Boolean,
  public val connectionsToClose: List<PeerKey>,
)
