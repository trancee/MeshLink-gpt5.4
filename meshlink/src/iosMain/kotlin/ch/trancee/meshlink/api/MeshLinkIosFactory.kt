package ch.trancee.meshlink.api

import ch.trancee.meshlink.crypto.Identity
import ch.trancee.meshlink.crypto.IosCryptoDelegate
import ch.trancee.meshlink.crypto.IosCryptoProvider
import ch.trancee.meshlink.crypto.requireIosCryptoDelegate
import ch.trancee.meshlink.engine.HandshakeConfig
import ch.trancee.meshlink.engine.MeshEngine
import ch.trancee.meshlink.engine.MeshEngineConfig
import ch.trancee.meshlink.transport.BleTransportConfig
import ch.trancee.meshlink.transport.IosBleTransport

public object MeshLinkIosFactory {
  public fun installCryptoDelegate(delegate: IosCryptoDelegate): Unit {
    ch.trancee.meshlink.crypto.IosCryptoRuntime.delegate = delegate
  }

  public fun create(): MeshLinkApi {
    return create(config = MeshLinkConfig.default())
  }

  public fun create(config: MeshLinkConfig): MeshLinkApi {
    requireIosCryptoDelegate()
    val cryptoProvider = IosCryptoProvider()
    val identity = Identity.generate(provider = cryptoProvider)
    val localPeerId = PeerIdHex.fromBytes(identity.keyHash.copyOf(4))
    return create(config = config, localPeerId = localPeerId, cryptoProvider = cryptoProvider)
  }

  internal fun create(
    config: MeshLinkConfig,
    localPeerId: PeerIdHex,
    cryptoProvider: IosCryptoProvider = IosCryptoProvider(),
  ): MeshLinkApi {
    requireIosCryptoDelegate()
    return MeshEngine.create(
      config =
        MeshEngineConfig(
          meshLinkConfig = config,
          bleTransportConfig = BleTransportConfig.default(),
          handshakeConfig = HandshakeConfig.default(),
          sweepIntervalMillis = MeshEngineConfig.default().sweepIntervalMillis,
        ),
      transport = IosBleTransport(localPeerId = localPeerId),
      cryptoProvider = cryptoProvider,
    )
  }
}
