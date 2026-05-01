package ch.trancee.meshlink.crypto

/** Factory for the current platform crypto backend. */
public object CryptoProviderFactory {
  /** Returns the default crypto provider for the active platform. */
  public fun create(): CryptoProvider {
    return createPlatformCryptoProvider()
  }
}

internal expect fun createPlatformCryptoProvider(): CryptoProvider
