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
    public fun connectAndSend_delegateToTheVirtualTransportImplementation(): Unit {
        // Arrange
        val localPeerId = PeerIdHex(value = "00112233")
        val remotePeerId = PeerIdHex(value = "44556677")
        val localTransport = AndroidBleTransport(localPeerId = localPeerId)
        val remoteTransport = VirtualMeshTransport(localPeerId = remotePeerId)
        localTransport.attachPeer(peerId = remotePeerId, transport = remoteTransport)
        val payload = byteArrayOf(0x01, 0x02)

        // Act
        localTransport.connect(peerId = remotePeerId)
        localTransport.send(peerId = remotePeerId, payload = payload)
        val actualPayload = remoteTransport.receivedFrames.replayCache.single()
        val connectedState = localTransport.isConnected(peerId = remotePeerId)
        localTransport.disconnect(peerId = remotePeerId)
        val disconnectedState = localTransport.isConnected(peerId = remotePeerId)

        // Assert
        assertTrue(actual = connectedState)
        assertFalse(actual = disconnectedState)
        assertContentEquals(expected = payload, actual = actualPayload)
    }
}
