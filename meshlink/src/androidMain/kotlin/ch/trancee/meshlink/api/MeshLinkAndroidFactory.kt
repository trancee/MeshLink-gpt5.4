package ch.trancee.meshlink.api

public object MeshLinkAndroidFactory {
  public fun create(): MeshLinkApi {
    return create(config = MeshLinkConfig.default())
  }

  public fun create(config: MeshLinkConfig): MeshLinkApi {
    return MeshLink.create(config = config)
  }
}
