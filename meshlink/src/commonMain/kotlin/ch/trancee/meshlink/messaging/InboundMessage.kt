package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

/** Deduplicated inbound application message. */
public data class InboundMessage(
  public val messageId: MessageIdKey,
  public val fromPeerId: PeerIdHex,
  public val payload: ByteArray,
)
