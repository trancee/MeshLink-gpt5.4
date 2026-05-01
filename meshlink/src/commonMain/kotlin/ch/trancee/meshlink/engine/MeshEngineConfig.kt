package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.MeshLinkConfig
import ch.trancee.meshlink.transport.BleTransportConfig

public data class MeshEngineConfig(
  public val meshLinkConfig: MeshLinkConfig,
  public val bleTransportConfig: BleTransportConfig,
  public val handshakeConfig: HandshakeConfig,
  public val sweepIntervalMillis: Long,
) {
  init {
    require(sweepIntervalMillis > 0) {
      "MeshEngineConfig sweepIntervalMillis must be greater than 0."
    }
  }

  public companion object {
    public fun default(): MeshEngineConfig {
      return MeshEngineConfig(
        meshLinkConfig = MeshLinkConfig.default(),
        bleTransportConfig = BleTransportConfig.default(),
        handshakeConfig = HandshakeConfig.default(),
        sweepIntervalMillis = 30_000L,
      )
    }
  }
}
