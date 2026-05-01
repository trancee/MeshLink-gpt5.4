package ch.trancee.meshlink.routing

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class RoutingFoundationTest {
  @Test
  public fun routingConfig_defaultExposesExpectedTimeoutsAndHopLimit(): Unit {
    // Arrange
    // Act
    val actual = RoutingConfig.default()

    // Assert
    assertEquals(expected = 30_000L, actual = actual.routeExpiryMillis)
    assertEquals(expected = 15_000L, actual = actual.peerTimeoutMillis)
    assertEquals(expected = 8, actual = actual.hopLimit)
  }

  @Test
  public fun routingConfig_rejectsInvalidValues(): Unit {
    // Arrange
    // Act
    val routeExpiryError =
      assertFailsWith<IllegalArgumentException> {
        RoutingConfig(routeExpiryMillis = 0L, peerTimeoutMillis = 1L, hopLimit = 1)
      }
    val peerTimeoutError =
      assertFailsWith<IllegalArgumentException> {
        RoutingConfig(routeExpiryMillis = 1L, peerTimeoutMillis = 0L, hopLimit = 1)
      }
    val hopLimitError =
      assertFailsWith<IllegalArgumentException> {
        RoutingConfig(routeExpiryMillis = 1L, peerTimeoutMillis = 1L, hopLimit = 0)
      }

    // Assert
    assertEquals(
      expected = "RoutingConfig routeExpiryMillis must be greater than 0.",
      actual = routeExpiryError.message,
    )
    assertEquals(
      expected = "RoutingConfig peerTimeoutMillis must be greater than 0.",
      actual = peerTimeoutError.message,
    )
    assertEquals(
      expected = "RoutingConfig hopLimit must be greater than 0.",
      actual = hopLimitError.message,
    )
  }

  @Test
  public fun peerInfo_retainsConstructorValues(): Unit {
    // Arrange
    val peerId = PeerIdHex(value = "00112233")

    // Act
    val actual = PeerInfo(peerId = peerId, metric = 3, lastSeenEpochMillis = 123L)

    // Assert
    assertEquals(expected = peerId, actual = actual.peerId)
    assertEquals(expected = 3, actual = actual.metric)
    assertEquals(expected = 123L, actual = actual.lastSeenEpochMillis)
  }

  @Test
  public fun peerInfo_rejectsInvalidMetricAndTimestampValues(): Unit {
    // Arrange
    // Act
    val metricError =
      assertFailsWith<IllegalArgumentException> {
        PeerInfo(peerId = PeerIdHex(value = "00112233"), metric = -1, lastSeenEpochMillis = 0L)
      }
    val timestampError =
      assertFailsWith<IllegalArgumentException> {
        PeerInfo(peerId = PeerIdHex(value = "00112233"), metric = 0, lastSeenEpochMillis = -1L)
      }

    // Assert
    assertEquals(
      expected = "PeerInfo metric must be greater than or equal to 0.",
      actual = metricError.message,
    )
    assertEquals(
      expected = "PeerInfo lastSeenEpochMillis must be greater than or equal to 0.",
      actual = timestampError.message,
    )
  }

  @Test
  public fun outboundFrame_retainsConstructorValues(): Unit {
    // Arrange
    val peerId = PeerIdHex(value = "44556677")
    val payload = byteArrayOf(0x01, 0x02)

    // Act
    val actual = OutboundFrame(nextHopPeerId = peerId, hopCount = 2, payload = payload)

    // Assert
    assertEquals(expected = peerId, actual = actual.nextHopPeerId)
    assertEquals(expected = 2, actual = actual.hopCount)
    assertContentEquals(expected = payload, actual = actual.payload)
  }

  @Test
  public fun outboundFrame_rejectsNegativeHopCounts(): Unit {
    // Arrange
    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        OutboundFrame(
          nextHopPeerId = PeerIdHex(value = "44556677"),
          hopCount = -1,
          payload = byteArrayOf(0x01),
        )
      }

    // Assert
    assertEquals(
      expected = "OutboundFrame hopCount must be greater than or equal to 0.",
      actual = error.message,
    )
  }
}
