package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class NoiseXXHandshakeTest {
    @Test
    public fun initiatorAndResponder_completeThreeMessageExchange(): Unit {
        // Arrange
        val initiator = NoiseXXHandshake(role = HandshakeRole.INITIATOR)
        val responder = NoiseXXHandshake(role = HandshakeRole.RESPONDER)

        // Act
        val messageOne: HandshakeMessage = initiator.createOutboundMessage(payload = byteArrayOf(0x11))
        responder.receiveInboundMessage(message = messageOne)
        val messageTwo: HandshakeMessage = responder.createOutboundMessage(payload = byteArrayOf(0x21, 0x22))
        initiator.receiveInboundMessage(message = messageTwo)
        val messageThree: HandshakeMessage = initiator.createOutboundMessage(payload = byteArrayOf(0x31, 0x32, 0x33))
        responder.receiveInboundMessage(message = messageThree)

        // Assert
        assertEquals(
            expected = HandshakeRound.ONE,
            actual = messageOne.round,
            message = "NoiseXXHandshake should label the initiator's first outbound message as round one",
        )
        assertEquals(
            expected = HandshakeRound.TWO,
            actual = messageTwo.round,
            message = "NoiseXXHandshake should label the responder's outbound message as round two",
        )
        assertEquals(
            expected = HandshakeRound.THREE,
            actual = messageThree.round,
            message = "NoiseXXHandshake should label the initiator's final outbound message as round three",
        )
        assertContentEquals(
            expected = byteArrayOf(0x31, 0x32, 0x33),
            actual = messageThree.payload,
            message = "NoiseXXHandshake should preserve outbound handshake payload bytes",
        )
        assertTrue(
            actual = initiator.isComplete(),
            message = "NoiseXXHandshake should mark the initiator flow complete after the third message is sent",
        )
        assertTrue(
            actual = responder.isComplete(),
            message = "NoiseXXHandshake should mark the responder flow complete after the third message is received",
        )
    }

    @Test
    public fun isComplete_returnsFalseBeforeAllRoundsFinish(): Unit {
        // Arrange
        val handshake = NoiseXXHandshake(role = HandshakeRole.INITIATOR)

        // Act
        val actual: Boolean = handshake.isComplete()

        // Assert
        assertFalse(
            actual = actual,
            message = "NoiseXXHandshake should report incomplete before any rounds are exchanged",
        )
    }

    @Test
    public fun createOutboundMessage_throwsWhenInitiatorTriesToSendTwiceInARow(): Unit {
        // Arrange
        val initiator = NoiseXXHandshake(role = HandshakeRole.INITIATOR)
        initiator.createOutboundMessage(payload = byteArrayOf(0x01))

        // Act
        val error = assertFailsWith<IllegalStateException> {
            initiator.createOutboundMessage(payload = byteArrayOf(0x02))
        }

        // Assert
        assertEquals(
            expected = "NoiseXXHandshake initiator cannot send in round 2.",
            actual = error.message,
            message = "NoiseXXHandshake should reject initiators that try to send twice before receiving round two",
        )
    }

    @Test
    public fun receiveInboundMessage_throwsWhenInitiatorAttemptsToReceiveBeforeSending(): Unit {
        // Arrange
        val initiator = NoiseXXHandshake(role = HandshakeRole.INITIATOR)
        val inbound = HandshakeMessage(
            round = HandshakeRound.ONE,
            payload = byteArrayOf(0x31),
        )

        // Act
        val error = assertFailsWith<IllegalStateException> {
            initiator.receiveInboundMessage(message = inbound)
        }

        // Assert
        assertEquals(
            expected = "NoiseXXHandshake initiator cannot receive in round 1.",
            actual = error.message,
            message = "NoiseXXHandshake should reject initiators that try to receive before sending round one",
        )
    }

    @Test
    public fun receiveInboundMessage_throwsWhenResponderAttemptsToReceiveTwiceBeforeSending(): Unit {
        // Arrange
        val responder = NoiseXXHandshake(role = HandshakeRole.RESPONDER)
        responder.receiveInboundMessage(
            message = HandshakeMessage(
                round = HandshakeRound.ONE,
                payload = byteArrayOf(0x41),
            ),
        )
        val secondInbound = HandshakeMessage(
            round = HandshakeRound.THREE,
            payload = byteArrayOf(0x42),
        )

        // Act
        val error = assertFailsWith<IllegalStateException> {
            responder.receiveInboundMessage(message = secondInbound)
        }

        // Assert
        assertEquals(
            expected = "NoiseXXHandshake responder cannot receive in round 2.",
            actual = error.message,
            message = "NoiseXXHandshake should reject responders that try to receive again before sending round two",
        )
    }

    @Test
    public fun receiveInboundMessage_throwsWhenRoundDoesNotMatchExpectedSequence(): Unit {
        // Arrange
        val responder = NoiseXXHandshake(role = HandshakeRole.RESPONDER)
        val unexpectedMessage = HandshakeMessage(
            round = HandshakeRound.TWO,
            payload = byteArrayOf(0x41),
        )

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            responder.receiveInboundMessage(message = unexpectedMessage)
        }

        // Assert
        assertEquals(
            expected = "NoiseXXHandshake expected ONE but received TWO for RESPONDER.",
            actual = error.message,
            message = "NoiseXXHandshake should reject inbound messages whose round does not match the expected XX sequence",
        )
    }

    @Test
    public fun createOutboundMessage_throwsWhenRoleCannotSendCurrentRound(): Unit {
        // Arrange
        val responder = NoiseXXHandshake(role = HandshakeRole.RESPONDER)

        // Act
        val error = assertFailsWith<IllegalStateException> {
            responder.createOutboundMessage(payload = byteArrayOf(0x51))
        }

        // Assert
        assertEquals(
            expected = "NoiseXXHandshake responder cannot send in round 1.",
            actual = error.message,
            message = "NoiseXXHandshake should reject outbound sends before the role reaches its send round",
        )
    }
}
