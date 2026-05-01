package ch.trancee.meshlink.power

/** Chooses which connections to shed when the target capacity drops. */
public object TieredShedder {
  public fun shed(
    connections: List<ManagedConnection>,
    targetConnectionCount: Int,
  ): List<ManagedConnection> {
    require(targetConnectionCount >= 0) {
      "TieredShedder targetConnectionCount must be greater than or equal to 0."
    }

    val overflowCount: Int = connections.size - targetConnectionCount
    if (overflowCount <= 0) {
      return emptyList()
    }

    return connections
      .sortedWith(
        compareBy<ManagedConnection> { connection ->
            priorityOf(transferStatus = connection.transferStatus)
          }
          .thenBy { connection -> connection.lastActivityEpochMillis }
      )
      .take(n = overflowCount)
  }

  private fun priorityOf(transferStatus: TransferStatus): Int {
    return when (transferStatus) {
      TransferStatus.COMPLETE -> 0
      TransferStatus.IDLE -> 1
      TransferStatus.IN_FLIGHT -> 2
    }
  }
}
