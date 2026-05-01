package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex

/** Tracks which peers are considered present based on recent observations. */
public class PresenceTracker(private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS) {
  private val peersById: MutableMap<String, Long> = mutableMapOf()

  init {
    require(timeoutMillis > 0) { "PresenceTracker timeoutMillis must be greater than 0." }
  }

  /**
   * Records a sighting of the peer and returns an [PresenceEvent.Appeared] only when the peer
   * transitions from absent to present.
   */
  public fun observe(peerId: PeerIdHex, nowEpochMillis: Long): PresenceEvent? {
    require(nowEpochMillis >= 0) {
      "PresenceTracker nowEpochMillis must be greater than or equal to 0."
    }

    val existingTimestamp: Long? = peersById.put(peerId.value, nowEpochMillis)
    return if (existingTimestamp == null) {
      PresenceEvent.Appeared(peerId = peerId)
    } else {
      null
    }
  }

  /** Expires peers that have not been observed within the timeout window. */
  public fun sweep(nowEpochMillis: Long): List<PresenceEvent> {
    require(nowEpochMillis >= 0) {
      "PresenceTracker nowEpochMillis must be greater than or equal to 0."
    }

    val disappearedPeers: List<PeerIdHex> =
      peersById.entries
        .filter { entry -> nowEpochMillis - entry.value >= timeoutMillis }
        .map { entry -> PeerIdHex(value = entry.key) }

    disappearedPeers.forEach { peerId -> peersById.remove(peerId.value) }

    return disappearedPeers.map { peerId -> PresenceEvent.Disappeared(peerId = peerId) }
  }

  /** Returns the current presence set. */
  public fun presentPeers(): Set<PeerIdHex> {
    return peersById.keys.mapTo(destination = linkedSetOf()) { peerId -> PeerIdHex(value = peerId) }
  }

  public companion object {
    public const val DEFAULT_TIMEOUT_MILLIS: Long = 15_000L
  }
}

/** Presence transition emitted by [PresenceTracker]. */
public sealed class PresenceEvent {
  public data class Appeared(public val peerId: PeerIdHex) : PresenceEvent()

  public data class Disappeared(public val peerId: PeerIdHex) : PresenceEvent()
}
