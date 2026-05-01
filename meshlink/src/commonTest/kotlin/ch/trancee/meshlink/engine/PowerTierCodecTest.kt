package ch.trancee.meshlink.engine

import ch.trancee.meshlink.transport.AdvertisementPowerTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class PowerTierCodecTest {
    @Test
    public fun encode_mapsHighNormalAndLowToStableWireCodes(): Unit {
        // Arrange
        // Act
        val high = PowerTierCodec.encode(powerTier = AdvertisementPowerTier.HIGH)
        val normal = PowerTierCodec.encode(powerTier = AdvertisementPowerTier.NORMAL)
        val low = PowerTierCodec.encode(powerTier = AdvertisementPowerTier.LOW)

        // Assert
        assertEquals(expected = 0x00, actual = high.toInt() and 0xFF)
        assertEquals(expected = 0x01, actual = normal.toInt() and 0xFF)
        assertEquals(expected = 0x02, actual = low.toInt() and 0xFF)
    }

    @Test
    public fun decode_roundTripsAllSupportedPowerTiers(): Unit {
        // Arrange
        // Act
        val high = PowerTierCodec.decode(encoded = 0x00)
        val normal = PowerTierCodec.decode(encoded = 0x01)
        val low = PowerTierCodec.decode(encoded = 0x02)

        // Assert
        assertEquals(expected = AdvertisementPowerTier.HIGH, actual = high)
        assertEquals(expected = AdvertisementPowerTier.NORMAL, actual = normal)
        assertEquals(expected = AdvertisementPowerTier.LOW, actual = low)
    }

    @Test
    public fun decode_rejectsUnknownWireCodes(): Unit {
        // Arrange
        // Act
        val error = assertFailsWith<IllegalArgumentException> {
            PowerTierCodec.decode(encoded = 0x7F)
        }

        // Assert
        assertEquals(
            expected = "PowerTierCodec encoded value 127 is not supported.",
            actual = error.message,
        )
    }
}
