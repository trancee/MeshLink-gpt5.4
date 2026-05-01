package ch.trancee.meshlink.crypto

/** Public/secret key material pair returned by a [CryptoProvider]. */
public data class KeyPair(public val publicKey: ByteArray, public val secretKey: ByteArray)
