package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.DeliveryAckMessage
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.KeepaliveMessage
import ch.trancee.meshlink.wire.messages.RoutedMessage
import ch.trancee.meshlink.wire.messages.UpdateMessage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

public class WireFormatTest {
    @Test
    public fun roundTrip_allCurrentlySupportedMessageTypes(): Unit {
        // Arrange
        val messages: List<WireMessage> = listOf(
            HelloMessage(
                peerId = byteArrayOf(0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B),
                appIdHash = 0x01020304,
            ),
            HandshakeMessage(
                round = HandshakeRound.THREE,
                payload = byteArrayOf(0x21, 0x22, 0x23),
            ),
            KeepaliveMessage,
            RoutedMessage(
                hopCount = 1u,
                maxHops = 4u,
                payload = byteArrayOf(0x31, 0x32),
            ),
            UpdateMessage(
                destinationPeerId = byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C),
                metric = 77,
                seqno = 8,
            ),
            DeliveryAckMessage(messageId = 0x0102030405060708),
        )

        // Act
        val actualRoundTrips: List<Pair<ByteArray, ByteArray>> = messages.map { message ->
            val encoded: ByteArray = WireCodec.encode(message = message)
            val decoded: WireMessage = WireCodec.decode(encoded = encoded)
            encoded to WireCodec.encode(message = decoded)
        }

        // Assert
        assertEquals(
            expected = messages.size,
            actual = actualRoundTrips.size,
            message = "WireFormatTest should round-trip every currently supported message type",
        )
        actualRoundTrips.forEachIndexed { index, roundTrip ->
            assertContentEquals(
                expected = roundTrip.first,
                actual = roundTrip.second,
                message = "WireCodec should re-encode message #$index deterministically after decode",
            )
        }
    }
}
