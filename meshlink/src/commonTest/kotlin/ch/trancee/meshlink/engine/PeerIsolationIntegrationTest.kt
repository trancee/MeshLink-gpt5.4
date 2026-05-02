package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.MeshLinkConfig
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.transport.AdvertisementCodec
import ch.trancee.meshlink.transport.VirtualMeshTransport
import ch.trancee.meshlink.wire.messages.HelloMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class PeerIsolationIntegrationTest {
  @Test
  public fun receiveInboundMessage_ignoresHelloMessagesFromDifferentApplicationMeshes(): Unit {
    // Arrange
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport =
          VirtualMeshTransport(localPeerId = ch.trancee.meshlink.api.PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    val foreignAppIdHash = AdvertisementCodec.applicationIdHash(applicationId = "other-app")

    // Act
    engine.receiveInboundMessage(
      peerId = ch.trancee.meshlink.api.PeerIdHex(value = "44556677"),
      message =
        HelloMessage(peerId = byteArrayOf(0x44, 0x55, 0x66, 0x77), appIdHash = foreignAppIdHash),
    )

    // Assert
    assertEquals(expected = emptyList(), actual = engine.peers.value)
  }

  @Test
  public fun receiveInboundMessage_acceptsHelloMessagesFromTheSameApplicationMeshOnce(): Unit {
    // Arrange
    val config = MeshLinkConfig { applicationId = "meshlink-chat" }
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default().copy(meshLinkConfig = config),
        transport =
          VirtualMeshTransport(localPeerId = ch.trancee.meshlink.api.PeerIdHex(value = "00112233")),
        cryptoProvider = FakeCryptoProvider(),
      )
    val localAppIdHash = AdvertisementCodec.applicationIdHash(applicationId = config.applicationId)
    val helloMessage =
      HelloMessage(peerId = byteArrayOf(0x44, 0x55, 0x66, 0x77), appIdHash = localAppIdHash)

    // Act
    engine.receiveInboundMessage(
      peerId = ch.trancee.meshlink.api.PeerIdHex(value = "44556677"),
      message = helloMessage,
    )
    engine.receiveInboundMessage(
      peerId = ch.trancee.meshlink.api.PeerIdHex(value = "44556677"),
      message = helloMessage,
    )
    val actual = engine.peers.value

    // Assert
    assertEquals(expected = 1, actual = actual.size)
    assertEquals(expected = PeerState.Discovered, actual = actual.single().state)
    assertTrue(
      actual = actual.single().peerId.value == "44556677",
      message = "PeerIsolationIntegrationTest should retain peers from the same application mesh.",
    )
  }
}
