package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.MeshLinkApi
import ch.trancee.meshlink.api.MeshLinkConfig
import ch.trancee.meshlink.api.MeshLinkIosFactory
import ch.trancee.meshlink.api.MeshLinkState
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.IosCryptoProvider

public class MeshNode(public val peerId: PeerIdHex, private val engine: MeshLinkApi) {
  internal constructor(
    peerId: PeerIdHex,
    config: MeshLinkConfig,
    cryptoProvider: IosCryptoProvider = IosCryptoProvider(),
  ) : this(
    peerId = peerId,
    engine =
      MeshLinkIosFactory.create(
        config = config,
        localPeerId = peerId,
        cryptoProvider = cryptoProvider,
      ),
  )

  public fun start(): Unit {
    engine.start()
  }

  public fun stop(): Unit {
    engine.stop()
  }

  public fun state(): MeshLinkState {
    return engine.state.value
  }
}
