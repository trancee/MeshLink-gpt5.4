package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class AdvertisementCodecTest {
  @Test
  public fun encodeAndDecode_roundTripPseudonymAndPowerTier(): Unit {
    // Arrange
    val pseudonym =
      byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B)
    val expectedPowerTier = AdvertisementPowerTier.NORMAL

    // Act
    val encoded = AdvertisementCodec.encode(pseudonym = pseudonym, powerTier = expectedPowerTier)
    val actual = AdvertisementCodec.decode(serviceData = encoded)

    // Assert
    assertContentEquals(expected = pseudonym, actual = actual.pseudonym)
    assertEquals(expected = expectedPowerTier, actual = actual.powerTier)
  }

  @Test
  public fun encode_fitsWithinTheBleAdvertisementBudget(): Unit {
    // Arrange
    val pseudonym =
      ByteArray(size = AdvertisementCodec.PSEUDONYM_LENGTH_BYTES) { index -> index.toByte() }

    // Act
    val actual =
      AdvertisementCodec.encode(pseudonym = pseudonym, powerTier = AdvertisementPowerTier.HIGH)

    // Assert
    assertTrue(
      actual = actual.size <= AdvertisementCodec.BLE_ADVERTISEMENT_LIMIT_BYTES,
      message = "AdvertisementCodec should fit within the BLE advertisement service-data limit",
    )
    assertEquals(expected = AdvertisementCodec.ENCODED_LENGTH_BYTES, actual = actual.size)
  }

  @Test
  public fun encode_rejectsPseudonymsWithTheWrongLength(): Unit {
    // Arrange
    val expectedMessage = "AdvertisementCodec pseudonym must be exactly 12 bytes."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        AdvertisementCodec.encode(
          pseudonym = ByteArray(size = 11),
          powerTier = AdvertisementPowerTier.LOW,
        )
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun decode_rejectsUnexpectedServiceDataLengths(): Unit {
    // Arrange
    val expectedMessage = "AdvertisementCodec serviceData must be exactly 13 bytes."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        AdvertisementCodec.decode(serviceData = ByteArray(size = 12))
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun decode_rejectsUnknownPowerTierCodes(): Unit {
    // Arrange
    val invalidPayload = ByteArray(size = AdvertisementCodec.ENCODED_LENGTH_BYTES)
    invalidPayload[AdvertisementCodec.ENCODED_LENGTH_BYTES - 1] = 0x7F

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        AdvertisementCodec.decode(serviceData = invalidPayload)
      }

    // Assert
    assertEquals(
      expected = "AdvertisementPowerTier wireCode 127 is not supported.",
      actual = error.message,
    )
  }

  @Test
  public fun fromWireCode_decodesAllSupportedPowerTiers(): Unit {
    // Arrange
    // Act
    val high = AdvertisementPowerTier.fromWireCode(wireCode = 0x00)
    val normal = AdvertisementPowerTier.fromWireCode(wireCode = 0x01)
    val low = AdvertisementPowerTier.fromWireCode(wireCode = 0x02)

    // Assert
    assertEquals(expected = AdvertisementPowerTier.HIGH, actual = high)
    assertEquals(expected = AdvertisementPowerTier.NORMAL, actual = normal)
    assertEquals(expected = AdvertisementPowerTier.LOW, actual = low)
  }
}
