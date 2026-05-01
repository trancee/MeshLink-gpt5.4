package ch.trancee.meshlink.harness

import ch.trancee.meshlink.api.MeshLinkState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class MeshTestHarnessTest {
  @Test
  public fun createConnected_buildsTwoConnectedEnginesWithDistinctPeerIds(): Unit {
    // Arrange
    val expectedFirstState = MeshLinkState.UNINITIALIZED
    val expectedSecondState = MeshLinkState.UNINITIALIZED

    // Act
    val harness = MeshTestHarness.createConnected()

    // Assert
    assertTrue(
      actual = harness.firstPeerId != harness.secondPeerId,
      message = "MeshTestHarness should allocate distinct peer IDs for each side.",
    )
    assertTrue(
      actual = harness.firstTransport.isConnected(peerId = harness.secondPeerId),
      message = "MeshTestHarness should connect the first transport to the second peer.",
    )
    assertTrue(
      actual = harness.secondTransport.isConnected(peerId = harness.firstPeerId),
      message = "MeshTestHarness should connect the second transport to the first peer.",
    )
    assertEquals(
      expected = expectedFirstState,
      actual = harness.firstEngine.state.value,
      message = "MeshTestHarness should create the first engine without starting it.",
    )
    assertEquals(
      expected = expectedSecondState,
      actual = harness.secondEngine.state.value,
      message = "MeshTestHarness should create the second engine without starting it.",
    )
  }
}
