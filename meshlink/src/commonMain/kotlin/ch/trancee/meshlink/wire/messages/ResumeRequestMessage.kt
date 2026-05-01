package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

/** Requests resumption of a transfer from a byte offset. */
public data class ResumeRequestMessage(public val transferId: Long, public val resumeOffset: Long) :
  WireMessage
