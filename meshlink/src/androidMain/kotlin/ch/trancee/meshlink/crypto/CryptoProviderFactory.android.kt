package ch.trancee.meshlink.crypto

internal actual fun createPlatformCryptoProvider(): CryptoProvider {
    return AndroidCryptoProvider()
}
