package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

/** Failed delivery outcome. */
public data class DeliveryFailed(
  public val messageId: MessageIdKey,
  public val peerId: PeerIdHex,
  public val reason: DeliveryFailureReason,
) : DeliveryOutcome

/** Canonical failure reasons for message delivery. */
public enum class DeliveryFailureReason {
  TIMEOUT,
  CANCELLED,
  UNREACHABLE,
  BUFFER_PRESSURE,
}
