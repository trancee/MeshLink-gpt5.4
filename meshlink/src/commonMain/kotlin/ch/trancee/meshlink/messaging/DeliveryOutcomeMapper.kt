package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

/** Small factory helpers for delivery outcomes. */
public object DeliveryOutcomeMapper {
  public fun failed(
    messageId: MessageIdKey,
    peerId: PeerIdHex,
    reason: DeliveryFailureReason,
  ): DeliveryFailed {
    return DeliveryFailed(messageId = messageId, peerId = peerId, reason = reason)
  }
}
