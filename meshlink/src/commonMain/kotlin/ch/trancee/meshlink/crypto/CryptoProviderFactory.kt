package ch.trancee.meshlink.crypto

public object CryptoProviderFactory {
  public fun create(): CryptoProvider {
    return createPlatformCryptoProvider()
  }
}

internal expect fun createPlatformCryptoProvider(): CryptoProvider
