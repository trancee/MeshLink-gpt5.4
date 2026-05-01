package ch.trancee.meshlink.wire.messages

import ch.trancee.meshlink.wire.WireMessage

public data class HelloMessage(
    public val peerId: ByteArray,
    public val appIdHash: Int,
) : WireMessage
