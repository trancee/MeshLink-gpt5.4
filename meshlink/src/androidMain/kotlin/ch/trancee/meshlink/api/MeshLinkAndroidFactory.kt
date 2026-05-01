package ch.trancee.meshlink.api

/** Android-specific convenience entry point mirroring the common factory shape. */
public object MeshLinkAndroidFactory {
  /** Creates a runtime using default configuration. */
  public fun create(): MeshLinkApi {
    return create(config = MeshLinkConfig.default())
  }

  /** Creates a runtime using the provided configuration. */
  public fun create(config: MeshLinkConfig): MeshLinkApi {
    return MeshLink.create(config = config)
  }
}
