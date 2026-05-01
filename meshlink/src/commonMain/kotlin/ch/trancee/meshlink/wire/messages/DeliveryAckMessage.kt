package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Acknowledges successful delivery of a message ID. */
public data class DeliveryAckMessage(public val messageId: Long) : WireMessage
