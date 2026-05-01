package ch.trancee.meshlink.api

import ch.trancee.meshlink.power.PowerTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class MeshHealthSnapshotTest {
  @Test
  public fun constructor_retainsOperationalSnapshotValues(): Unit {
    // Arrange
    val connectedPeers =
      listOf(
        PeerDetail(
          peerId = PeerIdHex(value = "00112233"),
          state = PeerState.Connected,
          displayName = "Alice",
          lastSeenEpochMillis = 10L,
        )
      )
    val expectedRoutingTableSize = 4
    val expectedActiveTransferCount = 2
    val expectedBufferedMessageCount = 1
    val expectedPowerTier = PowerTier.NORMAL

    // Act
    val actual =
      MeshHealthSnapshot(
        connectedPeers = connectedPeers,
        routingTableSize = expectedRoutingTableSize,
        activeTransferCount = expectedActiveTransferCount,
        bufferedMessageCount = expectedBufferedMessageCount,
        powerTier = expectedPowerTier,
      )

    // Assert
    assertEquals(expected = connectedPeers, actual = actual.connectedPeers)
    assertEquals(expected = connectedPeers.size, actual = actual.connectedPeerCount)
    assertEquals(expected = expectedRoutingTableSize, actual = actual.routingTableSize)
    assertEquals(expected = expectedActiveTransferCount, actual = actual.activeTransferCount)
    assertEquals(expected = expectedBufferedMessageCount, actual = actual.bufferedMessageCount)
    assertEquals(expected = expectedPowerTier, actual = actual.powerTier)
  }

  @Test
  public fun constructor_copiesConnectedPeersDefensively(): Unit {
    // Arrange
    val sourcePeers =
      mutableListOf(
        PeerDetail(
          peerId = PeerIdHex(value = "00112233"),
          state = PeerState.Connected,
          displayName = "Alice",
          lastSeenEpochMillis = 10L,
        )
      )

    // Act
    val snapshot =
      MeshHealthSnapshot(
        connectedPeers = sourcePeers,
        routingTableSize = 1,
        activeTransferCount = 0,
        bufferedMessageCount = 0,
        powerTier = PowerTier.HIGH,
      )
    sourcePeers +=
      PeerDetail(
        peerId = PeerIdHex(value = "44556677"),
        state = PeerState.Connected,
        displayName = "Bob",
        lastSeenEpochMillis = 20L,
      )

    // Assert
    assertEquals(
      expected = 1,
      actual = snapshot.connectedPeers.size,
      message = "MeshHealthSnapshot should not retain the caller's mutable peer list.",
    )
  }

  @Test
  public fun constructor_rejectsNegativeRoutingTableSize(): Unit {
    // Arrange
    val connectedPeers = emptyList<PeerDetail>()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        MeshHealthSnapshot(
          connectedPeers = connectedPeers,
          routingTableSize = -1,
          activeTransferCount = 0,
          bufferedMessageCount = 0,
          powerTier = PowerTier.LOW,
        )
      }

    // Assert
    assertEquals(
      expected = "MeshHealthSnapshot routingTableSize must be greater than or equal to 0.",
      actual = error.message,
    )
  }

  @Test
  public fun constructor_rejectsNegativeActiveTransferCount(): Unit {
    // Arrange
    val connectedPeers = emptyList<PeerDetail>()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        MeshHealthSnapshot(
          connectedPeers = connectedPeers,
          routingTableSize = 0,
          activeTransferCount = -1,
          bufferedMessageCount = 0,
          powerTier = PowerTier.LOW,
        )
      }

    // Assert
    assertEquals(
      expected = "MeshHealthSnapshot activeTransferCount must be greater than or equal to 0.",
      actual = error.message,
    )
  }

  @Test
  public fun constructor_rejectsNegativeBufferedMessageCount(): Unit {
    // Arrange
    val connectedPeers = emptyList<PeerDetail>()

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        MeshHealthSnapshot(
          connectedPeers = connectedPeers,
          routingTableSize = 0,
          activeTransferCount = 0,
          bufferedMessageCount = -1,
          powerTier = PowerTier.LOW,
        )
      }

    // Assert
    assertEquals(
      expected = "MeshHealthSnapshot bufferedMessageCount must be greater than or equal to 0.",
      actual = error.message,
    )
  }

  @Test
  public fun toString_summarizesOperationalCounts(): Unit {
    // Arrange
    val snapshot =
      MeshHealthSnapshot(
        connectedPeers = emptyList(),
        routingTableSize = 4,
        activeTransferCount = 2,
        bufferedMessageCount = 1,
        powerTier = PowerTier.NORMAL,
      )

    // Act
    val actual = snapshot.toString()

    // Assert
    assertTrue(
      actual = actual.contains("connectedPeerCount=0"),
      message = "MeshHealthSnapshot.toString() should include the connected peer count.",
    )
    assertTrue(
      actual = actual.contains("routingTableSize=4"),
      message = "MeshHealthSnapshot.toString() should include the routing table size.",
    )
    assertTrue(
      actual = actual.contains("powerTier=NORMAL"),
      message = "MeshHealthSnapshot.toString() should include the active power tier.",
    )
  }
}
