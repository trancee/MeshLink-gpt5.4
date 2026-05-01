package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class RoutedMessageCodecTest {
    @Test
    public fun encodeAndDecode_roundTripRoutedMessage(): Unit {
        // Arrange
        val expectedPayload: ByteArray = byteArrayOf(0x41, 0x42, 0x43)
        val message = RoutedMessage(
            hopCount = 1u,
            maxHops = 3u,
            payload = expectedPayload,
        )

        // Act
        val encoded: ByteArray = RoutedMessageCodec.encode(message = message)
        val decoded: RoutedMessage = RoutedMessageCodec.decode(payload = encoded)

        // Assert
        assertEquals(
            expected = 1u,
            actual = decoded.hopCount,
            message = "RoutedMessageCodec should preserve hopCount",
        )
        assertEquals(
            expected = 3u,
            actual = decoded.maxHops,
            message = "RoutedMessageCodec should preserve maxHops",
        )
        assertContentEquals(
            expected = expectedPayload,
            actual = decoded.payload,
            message = "RoutedMessageCodec should preserve the routed payload bytes",
        )
    }

    @Test
    public fun encode_writesHopHeaderBeforePayload(): Unit {
        // Arrange
        val message = RoutedMessage(
            hopCount = 2u,
            maxHops = 5u,
            payload = byteArrayOf(0x51, 0x52),
        )

        // Act
        val encoded: ByteArray = RoutedMessageCodec.encode(message = message)

        // Assert
        assertContentEquals(
            expected = byteArrayOf(0x02, 0x05, 0x51, 0x52),
            actual = encoded,
            message = "RoutedMessageCodec should write hopCount and maxHops before the routed payload",
        )
    }

    @Test
    public fun decode_throwsWhenPayloadDoesNotContainHopHeader(): Unit {
        // Arrange
        val payload: ByteArray = byteArrayOf(0x01)

        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            RoutedMessageCodec.decode(payload = payload)
        }

        // Assert
        assertEquals(
            expected = "ROUTED_MESSAGE payload must contain hopCount and maxHops bytes.",
            actual = error.message,
            message = "RoutedMessageCodec should reject payloads that do not contain both hop header bytes",
        )
    }
}
