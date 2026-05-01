package ch.trancee.meshlink.wire

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class MessageTypeTest {
    @Test
    public fun fromCode_returnsMatchingMessageType(): Unit {
        // Arrange
        val code: UByte = 0x01u

        // Act
        val actual: MessageType = MessageType.fromCode(code = code)

        // Assert
        assertEquals(
            expected = MessageType.HELLO,
            actual = actual,
            message = "MessageType should decode known one-byte message tags",
        )
    }

    @Test
    public fun fromCode_throwsForUnknownMessageTypeCode(): Unit {
        // Arrange
        val code: UByte = 0x7Fu

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            MessageType.fromCode(code = code)
        }

        // Assert
        assertEquals(
            expected = "Unknown message type code: 0x7f.",
            actual = error.message,
            message = "MessageType should reject unknown message type tags",
        )
    }
}
