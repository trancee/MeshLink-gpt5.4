package ch.trancee.meshlink.crypto.noise

import ch.trancee.meshlink.crypto.CryptoProvider
import ch.trancee.meshlink.crypto.KeyPair
import ch.trancee.meshlink.wire.messages.HandshakeMessage
import ch.trancee.meshlink.wire.messages.HandshakeRound
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

public class NoiseXXHandshakeTest {
  @Test
  public fun initiatorAndResponder_completeThreeMessageExchangeAndDeriveMatchingSessions(): Unit {
    // Arrange
    val initiator =
      NoiseXXHandshake(
        role = HandshakeRole.INITIATOR,
        provider =
          FakeHandshakeCryptoProvider(
            staticKeyPair = fakeKeyPair(id = 0x11),
            ephemeralKeyPair = fakeKeyPair(id = 0x12),
          ),
      )
    val responder =
      NoiseXXHandshake(
        role = HandshakeRole.RESPONDER,
        provider =
          FakeHandshakeCryptoProvider(
            staticKeyPair = fakeKeyPair(id = 0x21),
            ephemeralKeyPair = fakeKeyPair(id = 0x22),
          ),
      )
    val outboundPlaintext = byteArrayOf(0x41, 0x42, 0x43)
    val outboundAad = byteArrayOf(0x51)

    // Act
    val messageOne: HandshakeMessage = initiator.createOutboundMessage(payload = byteArrayOf(0x11))
    responder.receiveInboundMessage(message = messageOne)
    val messageTwo: HandshakeMessage =
      responder.createOutboundMessage(payload = byteArrayOf(0x21, 0x22))
    initiator.receiveInboundMessage(message = messageTwo)
    val messageThree: HandshakeMessage =
      initiator.createOutboundMessage(payload = byteArrayOf(0x31, 0x32, 0x33))
    responder.receiveInboundMessage(message = messageThree)
    val initiatorSession = initiator.transportSession()
    val responderSession = responder.transportSession()
    val ciphertext = initiatorSession!!.seal(aad = outboundAad, plaintext = outboundPlaintext)
    val actual = responderSession!!.open(aad = outboundAad, ciphertext = ciphertext)

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
      message =
        "NoiseXXHandshake should label the initiator's final outbound message as round three",
    )
    assertNotNull(
      actual = initiatorSession,
      message = "NoiseXXHandshake should expose a transport session once the initiator completes.",
    )
    assertNotNull(
      actual = responderSession,
      message = "NoiseXXHandshake should expose a transport session once the responder completes.",
    )
    assertContentEquals(
      expected = outboundPlaintext,
      actual = actual,
      message =
        "NoiseXXHandshake should derive matching transport sessions for initiator and responder.",
    )
    assertTrue(
      actual = initiator.isComplete(),
      message =
        "NoiseXXHandshake should mark the initiator flow complete after the third message is sent",
    )
    assertTrue(
      actual = responder.isComplete(),
      message =
        "NoiseXXHandshake should mark the responder flow complete after the third message is received",
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
    assertEquals(
      expected = null,
      actual = handshake.transportSession(),
      message = "NoiseXXHandshake should not expose a transport session before completion.",
    )
  }

  @Test
  public fun createOutboundMessage_throwsWhenInitiatorTriesToSendTwiceInARow(): Unit {
    // Arrange
    val initiator = NoiseXXHandshake(role = HandshakeRole.INITIATOR)
    initiator.createOutboundMessage(payload = byteArrayOf(0x01))

    // Act
    val error =
      assertFailsWith<IllegalStateException> {
        initiator.createOutboundMessage(payload = byteArrayOf(0x02))
      }

    // Assert
    assertEquals(
      expected = "NoiseXXHandshake initiator cannot send in round 2.",
      actual = error.message,
      message =
        "NoiseXXHandshake should reject initiators that try to send twice before receiving round two",
    )
  }

  @Test
  public fun receiveInboundMessage_throwsWhenInitiatorAttemptsToReceiveBeforeSending(): Unit {
    // Arrange
    val initiator = NoiseXXHandshake(role = HandshakeRole.INITIATOR)
    val inbound = HandshakeMessage(round = HandshakeRound.ONE, payload = byteArrayOf(0x31))

    // Act
    val error =
      assertFailsWith<IllegalStateException> { initiator.receiveInboundMessage(message = inbound) }

    // Assert
    assertEquals(
      expected = "NoiseXXHandshake initiator cannot receive in round 1.",
      actual = error.message,
      message =
        "NoiseXXHandshake should reject initiators that try to receive before sending round one",
    )
  }

  @Test
  public fun receiveInboundMessage_throwsWhenResponderAttemptsToReceiveTwiceBeforeSending(): Unit {
    // Arrange
    val responder = NoiseXXHandshake(role = HandshakeRole.RESPONDER)
    responder.receiveInboundMessage(
      message =
        HandshakeMessage(
          round = HandshakeRound.ONE,
          payload = ByteArray(size = X25519_KEY_SIZE) { 0x41 },
        )
    )
    val secondInbound = HandshakeMessage(round = HandshakeRound.THREE, payload = byteArrayOf(0x42))

    // Act
    val error =
      assertFailsWith<IllegalStateException> {
        responder.receiveInboundMessage(message = secondInbound)
      }

    // Assert
    assertEquals(
      expected = "NoiseXXHandshake responder cannot receive in round 2.",
      actual = error.message,
      message =
        "NoiseXXHandshake should reject responders that try to receive again before sending round two",
    )
  }

  @Test
  public fun receiveInboundMessage_throwsWhenRoundDoesNotMatchExpectedSequence(): Unit {
    // Arrange
    val responder = NoiseXXHandshake(role = HandshakeRole.RESPONDER)
    val unexpectedMessage =
      HandshakeMessage(round = HandshakeRound.TWO, payload = byteArrayOf(0x41))

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        responder.receiveInboundMessage(message = unexpectedMessage)
      }

    // Assert
    assertEquals(
      expected = "NoiseXXHandshake expected ONE but received TWO for RESPONDER.",
      actual = error.message,
      message =
        "NoiseXXHandshake should reject inbound messages whose round does not match the expected XX sequence",
    )
  }

  @Test
  public fun createOutboundMessage_throwsWhenRoleCannotSendCurrentRound(): Unit {
    // Arrange
    val responder = NoiseXXHandshake(role = HandshakeRole.RESPONDER)

    // Act
    val error =
      assertFailsWith<IllegalStateException> {
        responder.createOutboundMessage(payload = byteArrayOf(0x51))
      }

    // Assert
    assertEquals(
      expected = "NoiseXXHandshake responder cannot send in round 1.",
      actual = error.message,
      message =
        "NoiseXXHandshake should reject outbound sends before the role reaches its send round",
    )
  }

  private fun fakeKeyPair(id: Int): KeyPair {
    val encodedId: Byte = id.toByte()
    return KeyPair(
      publicKey = ByteArray(size = X25519_KEY_SIZE) { encodedId },
      secretKey = ByteArray(size = X25519_KEY_SIZE) { encodedId },
    )
  }

  private class FakeHandshakeCryptoProvider(staticKeyPair: KeyPair, ephemeralKeyPair: KeyPair) :
    CryptoProvider {
    private val x25519KeyPairs: ArrayDeque<KeyPair> =
      ArrayDeque(listOf(staticKeyPair, ephemeralKeyPair))

    override fun generateX25519KeyPair(): KeyPair {
      return x25519KeyPairs.removeFirst()
    }

    override fun generateEd25519KeyPair(): KeyPair = unsupported()

    override fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
      val firstId: Int = privateKey.first().toInt() and 0xFF
      val secondId: Int = publicKey.first().toInt() and 0xFF
      val low: Int = minOf(firstId, secondId)
      val high: Int = maxOf(firstId, secondId)
      return ByteArray(size = X25519_KEY_SIZE) { index ->
        if (index % 2 == 0) low.toByte() else high.toByte()
      }
    }

    override fun ed25519Sign(privateKey: ByteArray, message: ByteArray): ByteArray = unsupported()

    override fun ed25519Verify(
      publicKey: ByteArray,
      message: ByteArray,
      signature: ByteArray,
    ): Boolean = unsupported()

    override fun chaCha20Poly1305Encrypt(
      key: ByteArray,
      nonce: ByteArray,
      aad: ByteArray,
      plaintext: ByteArray,
    ): ByteArray {
      val mask: ByteArray = mask(key = key, nonce = nonce, aad = aad, size = plaintext.size)
      val cipherBody =
        ByteArray(size = plaintext.size) { index ->
          (plaintext[index].toInt() xor mask[index].toInt()).toByte()
        }
      val tag: ByteArray =
        hmacSha256(key = key, message = aad + nonce + cipherBody).copyOf(TAG_SIZE)
      return cipherBody + tag
    }

    override fun chaCha20Poly1305Decrypt(
      key: ByteArray,
      nonce: ByteArray,
      aad: ByteArray,
      ciphertext: ByteArray,
    ): ByteArray {
      val cipherBody: ByteArray = ciphertext.copyOfRange(0, ciphertext.size - TAG_SIZE)
      val actualTag: ByteArray = ciphertext.copyOfRange(ciphertext.size - TAG_SIZE, ciphertext.size)
      val expectedTag: ByteArray =
        hmacSha256(key = key, message = aad + nonce + cipherBody).copyOf(TAG_SIZE)
      require(actualTag.contentEquals(expectedTag)) {
        "FakeHandshakeCryptoProvider tag verification failed."
      }
      val mask: ByteArray = mask(key = key, nonce = nonce, aad = aad, size = cipherBody.size)
      return ByteArray(size = cipherBody.size) { index ->
        (cipherBody[index].toInt() xor mask[index].toInt()).toByte()
      }
    }

    override fun hkdfSha256(
      ikm: ByteArray,
      salt: ByteArray,
      info: ByteArray,
      outputLength: Int,
    ): ByteArray {
      val output = ByteArray(size = outputLength)
      var previousBlock = byteArrayOf()
      var generatedBytes = 0
      var counter = 1
      while (generatedBytes < outputLength) {
        previousBlock =
          hmacSha256(
            key = if (salt.isEmpty()) ByteArray(size = 32) else salt,
            message = previousBlock + info + ikm + byteArrayOf(counter.toByte()),
          )
        val bytesToCopy: Int = minOf(previousBlock.size, outputLength - generatedBytes)
        previousBlock.copyInto(
          destination = output,
          destinationOffset = generatedBytes,
          endIndex = bytesToCopy,
        )
        generatedBytes += bytesToCopy
        counter += 1
      }
      return output
    }

    override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
      return ByteArray(size = 32) { index ->
        val keyByte: Int = key.byteAtOrZero(index)
        val messageByte: Int = message.byteAtOrZero(index)
        ((keyByte + messageByte + index + key.size + message.size) and 0xFF).toByte()
      }
    }

    private fun mask(key: ByteArray, nonce: ByteArray, aad: ByteArray, size: Int): ByteArray {
      return hmacSha256(key = key, message = nonce + aad).let { seed ->
        ByteArray(size = size) { index -> seed[index % seed.size] }
      }
    }

    private fun ByteArray.byteAtOrZero(index: Int): Int {
      if (isEmpty()) {
        return 0
      }
      return this[index % size].toInt() and 0xFF
    }

    private fun <T> unsupported(): T {
      throw UnsupportedOperationException("Unused in test")
    }
  }

  private companion object {
    private const val TAG_SIZE: Int = 16
    private const val X25519_KEY_SIZE: Int = 32
  }
}
