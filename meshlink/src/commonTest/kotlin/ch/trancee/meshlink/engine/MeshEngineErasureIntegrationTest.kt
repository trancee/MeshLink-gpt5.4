package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerDetail
import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.PeerState
import ch.trancee.meshlink.routing.RoutingUpdate
import ch.trancee.meshlink.transfer.Priority
import ch.trancee.meshlink.transport.VirtualMeshTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

public class MeshEngineErasureIntegrationTest {
  @Test
  public fun forgetPeer_erasesPeerScopedRuntimeState(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val forgottenPeerId = PeerIdHex(value = "44556677")
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = localPeerId),
        cryptoProvider = FakeCryptoProvider(),
      )
    engine.publishPeers(
      peerDetails =
        listOf(
          PeerDetail(
            peerId = forgottenPeerId,
            state = PeerState.Connected,
            displayName = null,
            lastSeenEpochMillis = 0L,
          )
        )
    )
    engine.processRoutingUpdate(
      update =
        RoutingUpdate(
          destinationPeerId = forgottenPeerId,
          nextHopPeerId = forgottenPeerId,
          metric = 1,
          sequenceNumber = 5,
          expiresAtEpochMillis = 10_000L,
        )
    )
    engine.startTransfer(
      transferId = "transfer-1",
      recipientPeerId = forgottenPeerId,
      priority = Priority.NORMAL,
      payload = byteArrayOf(0x01, 0x02),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.sendRouted(
      peerId = PeerIdHex(value = "8899aabb"),
      payload = byteArrayOf(0x03),
      nowEpochMillis = 0L,
    )

    // Act
    engine.forgetPeer(peerId = forgottenPeerId)
    val snapshot = engine.healthSnapshot()

    // Assert
    assertTrue(actual = engine.peers.value.isEmpty())
    assertNull(actual = engine.sessionRegistry.session(peerId = forgottenPeerId))
    assertNull(actual = engine.nextHopFor(destinationPeerId = forgottenPeerId))
    assertEquals(expected = 0, actual = snapshot.connectedPeerCount)
    assertEquals(expected = 0, actual = snapshot.routingTableSize)
    assertEquals(expected = 0, actual = snapshot.activeTransferCount)
    assertEquals(
      expected = 1,
      actual = snapshot.bufferedMessageCount,
      message = "forgetPeer should erase only the targeted peer's state.",
    )
  }

  @Test
  public fun factoryReset_clearsAllRuntimeStateAfterStop(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val peerId = PeerIdHex(value = "44556677")
    val engine =
      MeshEngine.create(
        config = MeshEngineConfig.default(),
        transport = VirtualMeshTransport(localPeerId = localPeerId),
        cryptoProvider = FakeCryptoProvider(),
      )
    engine.publishPeers(
      peerDetails =
        listOf(
          PeerDetail(
            peerId = peerId,
            state = PeerState.Connected,
            displayName = null,
            lastSeenEpochMillis = 0L,
          )
        )
    )
    engine.processRoutingUpdate(
      update =
        RoutingUpdate(
          destinationPeerId = peerId,
          nextHopPeerId = peerId,
          metric = 1,
          sequenceNumber = 7,
          expiresAtEpochMillis = 10_000L,
        )
    )
    engine.startTransfer(
      transferId = "transfer-1",
      recipientPeerId = peerId,
      priority = Priority.HIGH,
      payload = byteArrayOf(0x01),
      preferL2cap = false,
      nowEpochMillis = 0L,
    )
    engine.sendRouted(
      peerId = PeerIdHex(value = "8899aabb"),
      payload = byteArrayOf(0x04),
      nowEpochMillis = 0L,
    )
    engine.start()
    engine.stop()

    // Act
    engine.factoryReset()
    val snapshot = engine.healthSnapshot()

    // Assert
    assertTrue(actual = engine.peers.value.isEmpty())
    assertEquals(expected = 0, actual = engine.sessionRegistry.snapshot().size)
    assertEquals(expected = 0, actual = snapshot.connectedPeerCount)
    assertEquals(expected = 0, actual = snapshot.routingTableSize)
    assertEquals(expected = 0, actual = snapshot.activeTransferCount)
    assertEquals(expected = 0, actual = snapshot.bufferedMessageCount)
  }
}
