package ch.trancee.meshlink.power

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class PowerFoundationTest {
    @Test
    public fun powerConfig_defaultExposesExpectedThresholdsAndProfiles(): Unit {
        // Arrange
        // Act
        val actual = PowerConfig.default()

        // Assert
        assertEquals(expected = 80, actual = actual.highTierThresholdPercent)
        assertEquals(expected = 30, actual = actual.normalTierThresholdPercent)
        assertEquals(expected = 60_000L, actual = actual.batteryPollIntervalMillis)
        assertEquals(expected = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8), actual = actual.highProfile)
        assertEquals(expected = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4), actual = actual.normalProfile)
        assertEquals(expected = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2), actual = actual.lowProfile)
    }

    @Test
    public fun powerConfig_andPowerProfile_rejectInvalidValues(): Unit {
        // Arrange
        // Act
        val highThresholdError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = 101,
                normalTierThresholdPercent = 30,
                batteryPollIntervalMillis = 1L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val negativeHighThresholdError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = -1,
                normalTierThresholdPercent = 30,
                batteryPollIntervalMillis = 1L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val normalThresholdError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = 80,
                normalTierThresholdPercent = -1,
                batteryPollIntervalMillis = 1L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val oversizedNormalThresholdError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = 80,
                normalTierThresholdPercent = 101,
                batteryPollIntervalMillis = 1L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val orderingError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = 30,
                normalTierThresholdPercent = 30,
                batteryPollIntervalMillis = 1L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val invertedOrderingError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = 20,
                normalTierThresholdPercent = 30,
                batteryPollIntervalMillis = 1L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val pollIntervalError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = 80,
                normalTierThresholdPercent = 30,
                batteryPollIntervalMillis = 0L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 5, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val lowDutyError = assertFailsWith<IllegalArgumentException> {
            PowerConfig(
                highTierThresholdPercent = 80,
                normalTierThresholdPercent = 30,
                batteryPollIntervalMillis = 1L,
                highProfile = PowerProfile(scanDutyCyclePercent = 100, connectionIntervalMillis = 100, maxConnections = 8),
                normalProfile = PowerProfile(scanDutyCyclePercent = 50, connectionIntervalMillis = 250, maxConnections = 4),
                lowProfile = PowerProfile(scanDutyCyclePercent = 6, connectionIntervalMillis = 500, maxConnections = 2),
            )
        }
        val dutyCycleError = assertFailsWith<IllegalArgumentException> {
            PowerProfile(scanDutyCyclePercent = 101, connectionIntervalMillis = 100, maxConnections = 1)
        }
        val negativeDutyCycleError = assertFailsWith<IllegalArgumentException> {
            PowerProfile(scanDutyCyclePercent = -1, connectionIntervalMillis = 100, maxConnections = 1)
        }
        val intervalError = assertFailsWith<IllegalArgumentException> {
            PowerProfile(scanDutyCyclePercent = 10, connectionIntervalMillis = 0, maxConnections = 1)
        }
        val maxConnectionsError = assertFailsWith<IllegalArgumentException> {
            PowerProfile(scanDutyCyclePercent = 10, connectionIntervalMillis = 1, maxConnections = -1)
        }

        // Assert
        assertEquals(expected = "PowerConfig highTierThresholdPercent must be between 0 and 100.", actual = highThresholdError.message)
        assertEquals(expected = "PowerConfig highTierThresholdPercent must be between 0 and 100.", actual = negativeHighThresholdError.message)
        assertEquals(expected = "PowerConfig normalTierThresholdPercent must be between 0 and 100.", actual = normalThresholdError.message)
        assertEquals(expected = "PowerConfig normalTierThresholdPercent must be between 0 and 100.", actual = oversizedNormalThresholdError.message)
        assertEquals(expected = "PowerConfig highTierThresholdPercent must be greater than normalTierThresholdPercent.", actual = orderingError.message)
        assertEquals(expected = "PowerConfig highTierThresholdPercent must be greater than normalTierThresholdPercent.", actual = invertedOrderingError.message)
        assertEquals(expected = "PowerConfig batteryPollIntervalMillis must be greater than 0.", actual = pollIntervalError.message)
        assertEquals(expected = "PowerConfig lowProfile.scanDutyCyclePercent must not exceed 5.", actual = lowDutyError.message)
        assertEquals(expected = "PowerProfile scanDutyCyclePercent must be between 0 and 100.", actual = dutyCycleError.message)
        assertEquals(expected = "PowerProfile scanDutyCyclePercent must be between 0 and 100.", actual = negativeDutyCycleError.message)
        assertEquals(expected = "PowerProfile connectionIntervalMillis must be greater than 0.", actual = intervalError.message)
        assertEquals(expected = "PowerProfile maxConnections must be greater than or equal to 0.", actual = maxConnectionsError.message)
    }

    @Test
    public fun powerTier_exposesTheThreeOperationalLevels(): Unit {
        // Arrange
        // Act
        val actual = PowerTier.entries

        // Assert
        assertEquals(expected = listOf(PowerTier.HIGH, PowerTier.NORMAL, PowerTier.LOW), actual = actual)
    }
}
