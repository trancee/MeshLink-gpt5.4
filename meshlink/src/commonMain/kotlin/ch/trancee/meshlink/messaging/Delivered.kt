package ch.trancee.meshlink.messaging

import ch.trancee.meshlink.api.PeerIdHex

public data class Delivered(public val messageId: MessageIdKey, public val peerId: PeerIdHex) :
  DeliveryOutcome
