package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class NackMessage(public val messageId: Long, public val reasonCode: Int) : WireMessage
