package ch.trancee.meshlink.crypto

/** Android actual that wires the shared crypto abstraction to the JVM provider. */
internal actual fun createPlatformCryptoProvider(): CryptoProvider {
  return AndroidCryptoProvider()
}
