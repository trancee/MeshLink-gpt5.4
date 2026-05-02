package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.crypto.TrustDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

public class MeshSessionRegistryTest {
  @Test
  public fun upsert_tracksSessionStateAndAggregateCounts(): Unit {
    // Arrange
    val registry = MeshSessionRegistry()
    val peerId = PeerIdHex(value = "00112233")
    val expectedTransferIds = setOf("transfer-1", "transfer-2")

    // Act
    registry.upsert(
      peerId = peerId,
      transportConnected = true,
      trustDecision = TrustDecision.Accepted,
      routeAvailable = true,
      activeTransferIds = expectedTransferIds,
    )
    val actual = registry.session(peerId = peerId)

    // Assert
    assertNotNull(actual, message = "MeshSessionRegistry should retain inserted peer state.")
    assertEquals(expected = peerId, actual = actual.peerId)
    assertEquals(expected = true, actual = actual.transportConnected)
    assertEquals(expected = TrustDecision.Accepted, actual = actual.trustDecision)
    assertEquals(expected = true, actual = actual.routeAvailable)
    assertEquals(expected = expectedTransferIds, actual = actual.activeTransferIds)
    assertEquals(
      expected = true,
      actual = actual.isTrusted,
      message = "MeshSessionRegistry should mark accepted peers as trusted.",
    )
    assertEquals(
      expected = listOf(peerId),
      actual = registry.connectedPeerIds(),
      message = "MeshSessionRegistry should report connected peers in insertion order.",
    )
    assertEquals(
      expected = expectedTransferIds.size,
      actual = registry.activeTransferCount(),
      message = "MeshSessionRegistry should sum active transfers across tracked peers.",
    )
  }

  @Test
  public fun remove_dropsTrackedStateForPeer(): Unit {
    // Arrange
    val registry = MeshSessionRegistry()
    val peerId = PeerIdHex(value = "00112233")
    registry.upsert(
      peerId = peerId,
      transportConnected = true,
      trustDecision = TrustDecision.Pinned,
      routeAvailable = false,
      activeTransferIds = setOf("transfer-1"),
    )

    // Act
    val removed = registry.remove(peerId = peerId)
    val remaining = registry.session(peerId = peerId)

    // Assert
    assertNotNull(removed, message = "MeshSessionRegistry should return the removed peer state.")
    assertEquals(expected = peerId, actual = removed.peerId)
    assertEquals(expected = true, actual = removed.isTrusted)
    assertNull(remaining, message = "MeshSessionRegistry should drop removed peer state.")
    assertEquals(expected = emptyList(), actual = registry.connectedPeerIds())
    assertEquals(expected = 0, actual = registry.activeTransferCount())
  }

  @Test
  public fun snapshotAndClear_exposeTrackedStateWithoutRetainingItAfterReset(): Unit {
    // Arrange
    val registry = MeshSessionRegistry()
    val peerId = PeerIdHex(value = "00112233")
    registry.upsert(
      peerId = peerId,
      transportConnected = false,
      trustDecision = TrustDecision.Rejected(reason = "prompt required"),
      routeAvailable = false,
      activeTransferIds = emptySet(),
    )

    // Act
    val snapshot = registry.snapshot()
    registry.clear()

    // Assert
    assertEquals(expected = 1, actual = snapshot.size)
    assertEquals(expected = peerId, actual = snapshot.single().peerId)
    assertEquals(
      expected = false,
      actual = snapshot.single().isTrusted,
      message = "MeshSessionRegistry should not mark rejected peers as trusted.",
    )
    assertEquals(expected = emptyList(), actual = registry.snapshot())
    assertEquals(expected = emptyList(), actual = registry.connectedPeerIds())
    assertEquals(expected = 0, actual = registry.activeTransferCount())
  }
}
