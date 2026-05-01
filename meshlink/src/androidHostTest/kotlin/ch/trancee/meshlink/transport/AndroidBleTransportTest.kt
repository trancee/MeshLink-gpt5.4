package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class AndroidBleTransportTest {
  @Test
  public fun advertise_updatesTheAndroidTransportAdvertisingState(): Unit {
    // Arrange
    val transport = AndroidBleTransport(localPeerId = PeerIdHex(value = "00112233"))

    // Act
    transport.advertise(enabled = true)
    val enabledState = transport.isAdvertising.value
    transport.advertise(enabled = false)
    val disabledState = transport.isAdvertising.value

    // Assert
    assertTrue(actual = enabledState)
    assertFalse(actual = disabledState)
  }

  @Test
  public fun connectAndSend_exchangeFramesBetweenAndroidPeersWithoutVirtualDelegation(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val localTransport = AndroidBleTransport(localPeerId = localPeerId)
    val remoteTransport = AndroidBleTransport(localPeerId = remotePeerId)
    localTransport.attachPeer(peerId = remotePeerId, transport = remoteTransport)
    remoteTransport.attachPeer(peerId = localPeerId, transport = localTransport)
    remoteTransport.advertise(enabled = true)
    val payload = byteArrayOf(0x01, 0x02)

    // Act
    localTransport.connect(peerId = remotePeerId)
    localTransport.send(peerId = remotePeerId, payload = payload)
    val actualPayload = remoteTransport.receivedFrames.replayCache.single()
    val localConnectedState = localTransport.isConnected(peerId = remotePeerId)
    val remoteConnectedState = remoteTransport.isConnected(peerId = localPeerId)
    localTransport.disconnect(peerId = remotePeerId)
    val localDisconnectedState = localTransport.isConnected(peerId = remotePeerId)
    val remoteDisconnectedState = remoteTransport.isConnected(peerId = localPeerId)

    // Assert
    assertTrue(actual = localConnectedState)
    assertTrue(actual = remoteConnectedState)
    assertFalse(actual = localDisconnectedState)
    assertFalse(actual = remoteDisconnectedState)
    assertContentEquals(expected = payload, actual = actualPayload)
  }

  @Test
  public fun connect_ignoresPeersThatAreNotAdvertising(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "44556677")
    val localTransport = AndroidBleTransport(localPeerId = localPeerId)
    val remoteTransport = AndroidBleTransport(localPeerId = remotePeerId)
    localTransport.attachPeer(peerId = remotePeerId, transport = remoteTransport)
    remoteTransport.attachPeer(peerId = localPeerId, transport = localTransport)

    // Act
    localTransport.connect(peerId = remotePeerId)
    val localConnectedState = localTransport.isConnected(peerId = remotePeerId)
    val remoteConnectedState = remoteTransport.isConnected(peerId = localPeerId)

    // Assert
    assertFalse(
      actual = localConnectedState,
      message = "AndroidBleTransport should only connect to peers that are advertising.",
    )
    assertFalse(
      actual = remoteConnectedState,
      message =
        "AndroidBleTransport should not mark the remote peer connected when discovery fails.",
    )
  }
}
