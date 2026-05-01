package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Negative acknowledgement carrying a message ID and reason code. */
public data class NackMessage(public val messageId: Long, public val reasonCode: Int) : WireMessage
