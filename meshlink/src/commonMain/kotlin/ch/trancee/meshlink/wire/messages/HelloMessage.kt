package ch.trancee.meshlink.wire.messages

public data class HelloMessage(
    public val peerId: ByteArray,
    public val appIdHash: Int,
)
