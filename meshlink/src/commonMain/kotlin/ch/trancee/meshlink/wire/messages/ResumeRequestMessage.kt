package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class ResumeRequestMessage(
    public val transferId: Long,
    public val resumeOffset: Long,
) : WireMessage
