package ch.trancee.meshlink.wire.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class RotationAnnouncementMessageCodecTest {
  @Test
  public fun encodeAndDecode_roundTripRotationAnnouncementMessage(): Unit {
    // Arrange
    val expectedPreviousPublicKey: ByteArray =
      ByteArray(size = 32) { index -> (index + 1).toByte() }
    val expectedNextPublicKey: ByteArray = ByteArray(size = 32) { index -> (index + 33).toByte() }
    val expectedSignature: ByteArray = ByteArray(size = 64) { index -> (index + 65).toByte() }
    val message =
      RotationAnnouncementMessage(
        previousPublicKey = expectedPreviousPublicKey,
        nextPublicKey = expectedNextPublicKey,
        signature = expectedSignature,
      )

    // Act
    val encoded: ByteArray = RotationAnnouncementMessageCodec.encode(message = message)
    val decoded: RotationAnnouncementMessage =
      RotationAnnouncementMessageCodec.decode(payload = encoded)

    // Assert
    assertContentEquals(
      expected = expectedPreviousPublicKey,
      actual = decoded.previousPublicKey,
      message = "RotationAnnouncementMessageCodec should preserve the previous public key",
    )
    assertContentEquals(
      expected = expectedNextPublicKey,
      actual = decoded.nextPublicKey,
      message = "RotationAnnouncementMessageCodec should preserve the next public key",
    )
    assertContentEquals(
      expected = expectedSignature,
      actual = decoded.signature,
      message = "RotationAnnouncementMessageCodec should preserve the rotation signature bytes",
    )
  }

  @Test
  public fun encode_throwsWhenPreviousKeyLengthIsInvalid(): Unit {
    // Arrange
    val message =
      RotationAnnouncementMessage(
        previousPublicKey = byteArrayOf(0x01),
        nextPublicKey = ByteArray(size = 32),
        signature = ByteArray(size = 64),
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        RotationAnnouncementMessageCodec.encode(message = message)
      }

    // Assert
    assertEquals(
      expected = "Rotation announcement previousPublicKey must be exactly 32 bytes.",
      actual = error.message,
      message = "RotationAnnouncementMessageCodec should reject malformed previous public key sizes",
    )
  }

  @Test
  public fun encode_throwsWhenNextKeyLengthIsInvalid(): Unit {
    // Arrange
    val message =
      RotationAnnouncementMessage(
        previousPublicKey = ByteArray(size = 32),
        nextPublicKey = byteArrayOf(0x01),
        signature = ByteArray(size = 64),
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        RotationAnnouncementMessageCodec.encode(message = message)
      }

    // Assert
    assertEquals(
      expected = "Rotation announcement nextPublicKey must be exactly 32 bytes.",
      actual = error.message,
      message = "RotationAnnouncementMessageCodec should reject malformed next public key sizes",
    )
  }

  @Test
  public fun encode_throwsWhenSignatureLengthIsInvalid(): Unit {
    // Arrange
    val message =
      RotationAnnouncementMessage(
        previousPublicKey = ByteArray(size = 32),
        nextPublicKey = ByteArray(size = 32),
        signature = byteArrayOf(0x01),
      )

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        RotationAnnouncementMessageCodec.encode(message = message)
      }

    // Assert
    assertEquals(
      expected = "Rotation announcement signature must be exactly 64 bytes.",
      actual = error.message,
      message = "RotationAnnouncementMessageCodec should reject malformed signature sizes",
    )
  }

  @Test
  public fun decode_throwsWhenPayloadLengthIsInvalid(): Unit {
    // Arrange
    val payload: ByteArray = byteArrayOf(0x01)

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        RotationAnnouncementMessageCodec.decode(payload = payload)
      }

    // Assert
    assertEquals(
      expected = "ROTATION_ANNOUNCEMENT payload must be exactly 128 bytes.",
      actual = error.message,
      message =
        "RotationAnnouncementMessageCodec should reject malformed ROTATION_ANNOUNCEMENT payload sizes",
    )
  }
}
