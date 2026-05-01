package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals

public class ApiDataTypesTest {
  @Test
  public fun peerDetail_retainsConstructorValues(): Unit {
    // Arrange
    val expectedPeerId = PeerIdHex(value = "0a0b")
    val expectedState: PeerState = PeerState.Connected
    val expectedDisplayName: String = "Alice"
    val expectedLastSeenEpochMillis: Long = 1234L

    // Act
    val actual =
      PeerDetail(
        peerId = expectedPeerId,
        state = expectedState,
        displayName = expectedDisplayName,
        lastSeenEpochMillis = expectedLastSeenEpochMillis,
      )

    // Assert
    assertEquals(expected = expectedPeerId, actual = actual.peerId)
    assertEquals(expected = expectedState, actual = actual.state)
    assertEquals(expected = expectedDisplayName, actual = actual.displayName)
    assertEquals(expected = expectedLastSeenEpochMillis, actual = actual.lastSeenEpochMillis)
  }

  @Test
  public fun routingSnapshot_retainsConstructorValues(): Unit {
    // Arrange
    val expectedDestinationPeerId = PeerIdHex(value = "0a0b")
    val expectedNextHopPeerId = PeerIdHex(value = "0c0d")
    val expectedHopCount: Int = 2
    val expectedMetric: Int = 42

    // Act
    val actual =
      RoutingSnapshot(
        destinationPeerId = expectedDestinationPeerId,
        nextHopPeerId = expectedNextHopPeerId,
        hopCount = expectedHopCount,
        metric = expectedMetric,
      )

    // Assert
    assertEquals(expected = expectedDestinationPeerId, actual = actual.destinationPeerId)
    assertEquals(expected = expectedNextHopPeerId, actual = actual.nextHopPeerId)
    assertEquals(expected = expectedHopCount, actual = actual.hopCount)
    assertEquals(expected = expectedMetric, actual = actual.metric)
  }
}
