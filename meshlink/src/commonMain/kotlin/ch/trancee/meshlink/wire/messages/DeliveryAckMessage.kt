package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class DeliveryAckMessage(
    public val messageId: Long,
) : WireMessage
