package ch.trancee.meshlink.crypto.noise

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class HandshakeStateTest {
  @Test
  public fun initiator_advancesThroughSendReceiveSendSequence(): Unit {
    // Arrange
    val state = HandshakeState(role = HandshakeRole.INITIATOR)

    // Act
    state.recordSend()
    state.recordReceive()
    state.recordSend()

    // Assert
    assertTrue(
      actual = state.isComplete(),
      message = "HandshakeState should mark the initiator flow complete after send/receive/send",
    )
  }

  @Test
  public fun responder_advancesThroughReceiveSendReceiveSequence(): Unit {
    // Arrange
    val state = HandshakeState(role = HandshakeRole.RESPONDER)

    // Act
    state.recordReceive()
    state.recordSend()
    state.recordReceive()

    // Assert
    assertTrue(
      actual = state.isComplete(),
      message = "HandshakeState should mark the responder flow complete after receive/send/receive",
    )
  }

  @Test
  public fun canSendAndCanReceive_reflectCurrentRoleAndRound(): Unit {
    // Arrange
    val initiator = HandshakeState(role = HandshakeRole.INITIATOR)
    val responder = HandshakeState(role = HandshakeRole.RESPONDER)

    // Act
    val initiatorCanSendInitially: Boolean = initiator.canSend()
    val initiatorCanReceiveInitially: Boolean = initiator.canReceive()
    val responderCanSendInitially: Boolean = responder.canSend()
    val responderCanReceiveInitially: Boolean = responder.canReceive()

    // Assert
    assertTrue(
      actual = initiatorCanSendInitially,
      message = "HandshakeState initiators should send first in Noise XX",
    )
    assertFalse(
      actual = initiatorCanReceiveInitially,
      message = "HandshakeState initiators should not receive before sending round one",
    )
    assertFalse(
      actual = responderCanSendInitially,
      message = "HandshakeState responders should not send before receiving round one",
    )
    assertTrue(
      actual = responderCanReceiveInitially,
      message = "HandshakeState responders should receive first in Noise XX",
    )
  }

  @Test
  public fun isComplete_returnsFalseUntilAllThreeRoundsFinish(): Unit {
    // Arrange
    val state = HandshakeState(role = HandshakeRole.INITIATOR)

    // Act
    val actual: Boolean = state.isComplete()

    // Assert
    assertFalse(
      actual = actual,
      message = "HandshakeState should report incomplete before all three Noise XX rounds finish",
    )
  }

  @Test
  public fun canSend_updatesAfterRoundTransitions(): Unit {
    // Arrange
    val initiator = HandshakeState(role = HandshakeRole.INITIATOR)
    val responder = HandshakeState(role = HandshakeRole.RESPONDER)
    initiator.recordSend()
    responder.recordReceive()

    // Act
    val initiatorCanSendAfterRoundOne: Boolean = initiator.canSend()
    val responderCanSendAfterRoundOne: Boolean = responder.canSend()

    // Assert
    assertFalse(
      actual = initiatorCanSendAfterRoundOne,
      message =
        "HandshakeState initiators should not send twice in a row before receiving round two",
    )
    assertTrue(
      actual = responderCanSendAfterRoundOne,
      message = "HandshakeState responders should send after receiving round one",
    )
  }

  @Test
  public fun currentRound_tracksProgressThroughHandshake(): Unit {
    // Arrange
    val state = HandshakeState(role = HandshakeRole.INITIATOR)
    state.recordSend()

    // Act
    val actual: Int = state.currentRound()

    // Assert
    assertEquals(
      expected = 2,
      actual = actual,
      message = "HandshakeState should advance the round number after each recorded action",
    )
  }

  @Test
  public fun recordSend_throwsWhenCurrentRoleMustReceive(): Unit {
    // Arrange
    val state = HandshakeState(role = HandshakeRole.RESPONDER)

    // Act
    val error = assertFailsWith<IllegalStateException> { state.recordSend() }

    // Assert
    assertEquals(
      expected = "HandshakeState cannot send in round 1 for role RESPONDER.",
      actual = error.message,
      message = "HandshakeState should reject invalid responder send transitions",
    )
  }

  @Test
  public fun recordReceive_throwsWhenCurrentRoleMustSend(): Unit {
    // Arrange
    val state = HandshakeState(role = HandshakeRole.INITIATOR)

    // Act
    val error = assertFailsWith<IllegalStateException> { state.recordReceive() }

    // Assert
    assertEquals(
      expected = "HandshakeState cannot receive in round 1 for role INITIATOR.",
      actual = error.message,
      message = "HandshakeState should reject invalid initiator receive transitions",
    )
  }
}
