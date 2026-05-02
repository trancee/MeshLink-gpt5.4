package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.PeerIdHex
import ch.trancee.meshlink.api.TrustMode
import ch.trancee.meshlink.crypto.noise.HandshakeRole
import ch.trancee.meshlink.crypto.noise.NoiseSession
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

public class NoiseHandshakeManagerIntegrationTest {
  @Test
  public fun tofuHandshake_completesAndDerivesMatchingSessionsForBothPeers(): Unit {
    // Arrange
    val peerId = PeerIdHex(value = "00112233")
    val initiator = NoiseHandshakeManager()
    val responder = NoiseHandshakeManager()
    val aad = byteArrayOf(0x51)
    val plaintext = byteArrayOf(0x61, 0x62, 0x63)

    // Act
    val transcript =
      exchangeHandshake(initiator = initiator, responder = responder, peerId = peerId)
    responder.receiveHandshakeMessage(
      peerId = peerId,
      role = HandshakeRole.RESPONDER,
      message = transcript.third,
    )
    val initiatorSession: NoiseSession? = initiator.session(peerId = peerId)
    val responderSession: NoiseSession? = responder.session(peerId = peerId)
    val ciphertext = initiatorSession!!.seal(aad = aad, plaintext = plaintext)
    val actual = responderSession!!.open(aad = aad, ciphertext = ciphertext)

    // Assert
    assertFalse(actual = initiator.isHandshakeActive(peerId = peerId))
    assertFalse(actual = responder.isHandshakeActive(peerId = peerId))
    assertNotNull(
      actual = initiatorSession,
      message =
        "NoiseHandshakeManager should retain the initiator transport session after TOFU acceptance.",
    )
    assertNotNull(
      actual = responderSession,
      message =
        "NoiseHandshakeManager should retain the responder transport session after TOFU acceptance.",
    )
    assertContentEquals(
      expected = plaintext,
      actual = actual,
      message =
        "NoiseHandshakeManager should derive interoperable sessions for both handshake participants.",
    )
  }

  @Test
  public fun strictHandshake_rejectsUnknownPeerAndDoesNotRetainResponderSession(): Unit {
    // Arrange
    val peerId = PeerIdHex(value = "00112233")
    val initiator = NoiseHandshakeManager()
    val responder = NoiseHandshakeManager(trustMode = TrustMode.STRICT)
    val transcript =
      exchangeHandshake(initiator = initiator, responder = responder, peerId = peerId)

    // Act
    val error =
      kotlin.test.assertFailsWith<IllegalStateException> {
        responder.receiveHandshakeMessage(
          peerId = peerId,
          role = HandshakeRole.RESPONDER,
          message = transcript.third,
        )
      }

    // Assert
    assertEquals(
      expected = "NoiseHandshakeManager rejected peer 00112233: Peer is not pinned in STRICT mode.",
      actual = error.message,
    )
    assertFalse(actual = responder.isHandshakeActive(peerId = peerId))
    assertNull(
      actual = responder.session(peerId = peerId),
      message =
        "NoiseHandshakeManager should not retain a session when strict trust rejects the peer.",
    )
  }

  @Test
  public fun promptHandshake_requiresApprovalAndDoesNotRetainResponderSession(): Unit {
    // Arrange
    val peerId = PeerIdHex(value = "00112233")
    val initiator = NoiseHandshakeManager()
    val responder = NoiseHandshakeManager(trustMode = TrustMode.PROMPT)
    val transcript =
      exchangeHandshake(initiator = initiator, responder = responder, peerId = peerId)

    // Act
    val error =
      kotlin.test.assertFailsWith<IllegalStateException> {
        responder.receiveHandshakeMessage(
          peerId = peerId,
          role = HandshakeRole.RESPONDER,
          message = transcript.third,
        )
      }

    // Assert
    assertEquals(
      expected = "NoiseHandshakeManager requires trust confirmation for peer 00112233.",
      actual = error.message,
    )
    assertFalse(actual = responder.isHandshakeActive(peerId = peerId))
    assertNull(
      actual = responder.session(peerId = peerId),
      message =
        "NoiseHandshakeManager should not retain a session when external trust approval is required.",
    )
  }

  private fun exchangeHandshake(
    initiator: NoiseHandshakeManager,
    responder: NoiseHandshakeManager,
    peerId: PeerIdHex,
  ): HandshakeTranscript {
    val first: HandshakeMessage =
      initiator.beginHandshake(
        peerId = peerId,
        role = HandshakeRole.INITIATOR,
        payload = byteArrayOf(0x01),
      )
    responder.receiveHandshakeMessage(
      peerId = peerId,
      role = HandshakeRole.RESPONDER,
      message = first,
    )
    val second: HandshakeMessage =
      responder.createOutboundMessage(peerId = peerId, payload = byteArrayOf(0x02))
    initiator.receiveHandshakeMessage(
      peerId = peerId,
      role = HandshakeRole.INITIATOR,
      message = second,
    )
    val third: HandshakeMessage =
      initiator.createOutboundMessage(peerId = peerId, payload = byteArrayOf(0x03))
    return HandshakeTranscript(first = first, second = second, third = third)
  }

  private data class HandshakeTranscript(
    val first: HandshakeMessage,
    val second: HandshakeMessage,
    val third: HandshakeMessage,
  )
}
