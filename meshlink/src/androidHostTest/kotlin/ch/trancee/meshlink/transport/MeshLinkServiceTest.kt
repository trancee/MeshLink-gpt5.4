package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class MeshLinkServiceTest {
  @Test
  public fun startAdvertising_returnsFalseWhenNoTransportIsInstalled(): Unit {
    // Arrange
    val service = MeshLinkService()

    // Act
    val actual = service.startAdvertising()

    // Assert
    assertFalse(actual = actual)
    assertFalse(actual = service.hasTransport())
  }

  @Test
  public fun installTransport_allowsAdvertisingToBeControlledThroughTheService(): Unit {
    // Arrange
    val transport = AndroidBleTransport(localPeerId = PeerIdHex(value = "00112233"))
    val service = MeshLinkService()
    service.installTransport(transport = transport)

    // Act
    val startResult = service.startAdvertising()
    val advertisingAfterStart = transport.isAdvertising.value
    val stopResult = service.stopAdvertising()
    val advertisingAfterStop = transport.isAdvertising.value

    // Assert
    assertTrue(actual = service.hasTransport())
    assertTrue(actual = startResult)
    assertTrue(actual = advertisingAfterStart)
    assertTrue(actual = stopResult)
    assertFalse(actual = advertisingAfterStop)
  }

  @Test
  public fun clearTransport_removesTheInstalledTransport(): Unit {
    // Arrange
    val service =
      MeshLinkService(transport = AndroidBleTransport(localPeerId = PeerIdHex(value = "00112233")))

    // Act
    service.clearTransport()
    val actual = service.stopAdvertising()

    // Assert
    assertFalse(actual = service.hasTransport())
    assertFalse(actual = actual)
  }
}
