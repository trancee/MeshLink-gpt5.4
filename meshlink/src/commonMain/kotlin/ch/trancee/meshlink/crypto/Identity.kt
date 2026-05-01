package ch.trancee.meshlink.crypto

public data class Identity(
    public val publicKey: ByteArray,
    public val secretKey: ByteArray,
    public val keyHash: ByteArray,
)
