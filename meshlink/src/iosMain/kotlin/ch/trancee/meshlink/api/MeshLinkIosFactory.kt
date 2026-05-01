package ch.trancee.meshlink.api

import ch.trancee.meshlink.crypto.IosCryptoDelegate
import ch.trancee.meshlink.crypto.IosCryptoRuntime

public object MeshLinkIosFactory {
  public fun installCryptoDelegate(delegate: IosCryptoDelegate): Unit {
    IosCryptoRuntime.delegate = delegate
  }

  public fun create(): MeshLinkApi {
    return create(config = MeshLinkConfig.default())
  }

  public fun create(config: MeshLinkConfig): MeshLinkApi {
    return MeshLink.create(config = config)
  }
}
