package ch.trancee.meshlink.wire

import ch.trancee.meshlink.wire.messages.BroadcastMessage
import ch.trancee.meshlink.wire.messages.DeliveryAckMessage
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import ch.trancee.meshlink.wire.messages.HelloMessage
import ch.trancee.meshlink.wire.messages.KeepaliveMessage
import ch.trancee.meshlink.wire.messages.NackMessage
import ch.trancee.meshlink.wire.messages.ResumeRequestMessage
import ch.trancee.meshlink.wire.messages.RotationAnnouncementMessage
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
            NackMessage(
                messageId = 0x2122232425262728,
                reasonCode = 502,
            ),
            ResumeRequestMessage(
                transferId = 0x3132333435363738,
                resumeOffset = 0x4142434445464748,
            ),
            BroadcastMessage(
                originPeerId = byteArrayOf(0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C),
                sequenceNumber = 9,
                maxHops = 3u,
                payload = byteArrayOf(0x61, 0x62),
            ),
            RotationAnnouncementMessage(
                previousPublicKey = ByteArray(size = 32) { index -> (index + 1).toByte() },
                nextPublicKey = ByteArray(size = 32) { index -> (index + 33).toByte() },
                signature = ByteArray(size = 64) { index -> (index + 65).toByte() },
            ),
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
