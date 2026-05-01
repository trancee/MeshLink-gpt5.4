package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals

public class RegulatoryRegionClampingTest {
  @Test
  public fun eu_clampsAdvertisementIntervalToThreeHundredMilliseconds(): Unit {
    // Arrange
    val region = RegulatoryRegion.EU

    // Act
    val actual: Int = region.clampAdvertisementIntervalMs(intervalMs = 150)

    // Assert
    assertEquals(
      expected = 300,
      actual = actual,
      message = "RegulatoryRegion.EU should clamp advertisement intervals to at least 300 ms",
    )
  }

  @Test
  public fun eu_clampsScanDutyCycleToSeventyPercent(): Unit {
    // Arrange
    val region = RegulatoryRegion.EU

    // Act
    val actual: Int = region.clampScanDutyCyclePercent(percent = 95)

    // Assert
    assertEquals(
      expected = 70,
      actual = actual,
      message = "RegulatoryRegion.EU should clamp scan duty cycle to 70%",
    )
  }

  @Test
  public fun worldwide_preservesValuesAlreadyWithinBounds(): Unit {
    // Arrange
    val region = RegulatoryRegion.WORLDWIDE

    // Act
    val actualInterval: Int = region.clampAdvertisementIntervalMs(intervalMs = 150)
    val actualDutyCycle: Int = region.clampScanDutyCyclePercent(percent = 50)

    // Assert
    assertEquals(expected = 150, actual = actualInterval)
    assertEquals(expected = 50, actual = actualDutyCycle)
  }

  @Test
  public fun clampScanDutyCyclePercent_neverReturnsNegativeValues(): Unit {
    // Arrange
    val region = RegulatoryRegion.US

    // Act
    val actual: Int = region.clampScanDutyCyclePercent(percent = -10)

    // Assert
    assertEquals(
      expected = 0,
      actual = actual,
      message = "RegulatoryRegion should clamp scan duty cycle to a minimum of 0%",
    )
  }
}
