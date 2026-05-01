package ch.trancee.meshlink.transport

import ch.trancee.meshlink.api.PeerIdHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class ConnectionInitiationPolicyTest {
  @Test
  public fun shouldLocalPeerInitiate_returnsTrueWhenTheLocalPeerIdSortsFirst(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "00112233")
    val remotePeerId = PeerIdHex(value = "8899aabb")

    // Act
    val actual =
      ConnectionInitiationPolicy.shouldLocalPeerInitiate(
        localPeerId = localPeerId,
        remotePeerId = remotePeerId,
      )

    // Assert
    assertTrue(
      actual = actual,
      message =
        "ConnectionInitiationPolicy should make the lexicographically smaller peer initiate the connection",
    )
  }

  @Test
  public fun shouldLocalPeerInitiate_returnsFalseWhenTheRemotePeerIdSortsFirst(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "8899aabb")
    val remotePeerId = PeerIdHex(value = "00112233")

    // Act
    val actual =
      ConnectionInitiationPolicy.shouldLocalPeerInitiate(
        localPeerId = localPeerId,
        remotePeerId = remotePeerId,
      )

    // Assert
    assertFalse(
      actual = actual,
      message =
        "ConnectionInitiationPolicy should avoid simultaneous connects by picking the same initiator on both peers",
    )
  }

  @Test
  public fun shouldLocalPeerInitiate_returnsFalseWhenPeerIdsAreEqual(): Unit {
    // Arrange
    val peerId = PeerIdHex(value = "00112233")

    // Act
    val actual =
      ConnectionInitiationPolicy.shouldLocalPeerInitiate(
        localPeerId = peerId,
        remotePeerId = peerId,
      )

    // Assert
    assertFalse(
      actual = actual,
      message =
        "ConnectionInitiationPolicy should not initiate a connection when both peer identifiers are identical",
    )
  }

  @Test
  public fun shouldLocalPeerInitiate_normalizesCaseBeforeComparingPeerIds(): Unit {
    // Arrange
    val localPeerId = PeerIdHex(value = "AABBCCDD")
    val remotePeerId = PeerIdHex(value = "bbccddee")

    // Act
    val actual =
      ConnectionInitiationPolicy.shouldLocalPeerInitiate(
        localPeerId = localPeerId,
        remotePeerId = remotePeerId,
      )

    // Assert
    assertTrue(
      actual = actual,
      message = "ConnectionInitiationPolicy should compare peer identifiers case-insensitively",
    )
  }

  @Test
  public fun shouldLocalPeerInitiate_isSymmetricAcrossBothPeers(): Unit {
    // Arrange
    val firstPeerId = PeerIdHex(value = "01020304")
    val secondPeerId = PeerIdHex(value = "0a0b0c0d")

    // Act
    val firstDecision =
      ConnectionInitiationPolicy.shouldLocalPeerInitiate(
        localPeerId = firstPeerId,
        remotePeerId = secondPeerId,
      )
    val secondDecision =
      ConnectionInitiationPolicy.shouldLocalPeerInitiate(
        localPeerId = secondPeerId,
        remotePeerId = firstPeerId,
      )

    // Assert
    assertEquals(expected = true, actual = firstDecision)
    assertEquals(expected = false, actual = secondDecision)
  }

  @Test
  public fun preferredDataPath_usesGattWhenFreshCacheMarksL2capUnsupported(): Unit {
    // Arrange
    val cachedCapability = OemL2capProbeResult(supportsL2cap = false, isStale = false)

    // Act
    val actual =
      ConnectionInitiationPolicy.preferredDataPath(
        preferL2cap = true,
        cachedCapability = cachedCapability,
      )

    // Assert
    assertEquals(
      expected = TransportDataPath.GATT,
      actual = actual,
      message =
        "ConnectionInitiationPolicy should fall back to GATT when a fresh cache entry says L2CAP is unavailable.",
    )
  }

  @Test
  public fun preferredDataPath_retriesL2capWhenTheNegativeCacheEntryIsStale(): Unit {
    // Arrange
    val cachedCapability = OemL2capProbeResult(supportsL2cap = false, isStale = true)

    // Act
    val actual =
      ConnectionInitiationPolicy.preferredDataPath(
        preferL2cap = true,
        cachedCapability = cachedCapability,
      )

    // Assert
    assertEquals(
      expected = TransportDataPath.L2CAP,
      actual = actual,
      message =
        "ConnectionInitiationPolicy should retry L2CAP once the cached negative capability has gone stale.",
    )
  }

  @Test
  public fun fallbackDataPath_demotesL2capFailuresToGatt(): Unit {
    // Arrange
    val failedDataPath = TransportDataPath.L2CAP

    // Act
    val actual = ConnectionInitiationPolicy.fallbackDataPath(failedDataPath = failedDataPath)

    // Assert
    assertEquals(
      expected = TransportDataPath.GATT,
      actual = actual,
      message = "ConnectionInitiationPolicy should demote failed L2CAP attempts to GATT.",
    )
  }
}
