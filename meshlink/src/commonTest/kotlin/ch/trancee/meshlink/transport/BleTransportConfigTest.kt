package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class BleTransportConfigTest {
  @Test
  public fun default_exposesExpectedFoundationValues(): Unit {
    // Arrange
    // Act
    val actual = BleTransportConfig.default()

    // Assert
    assertEquals(expected = 5_000, actual = actual.scanIntervalMillis)
    assertEquals(expected = 2_500, actual = actual.scanWindowMillis)
    assertEquals(expected = 15_000, actual = actual.connectionTimeoutMillis)
    assertEquals(expected = 300, actual = actual.advertisementIntervalMillis)
  }

  @Test
  public fun init_rejectsNonPositiveScanIntervals(): Unit {
    // Arrange
    val expectedMessage = "BleTransportConfig scanIntervalMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        BleTransportConfig(
          scanIntervalMillis = 0,
          scanWindowMillis = 1,
          connectionTimeoutMillis = 1,
          advertisementIntervalMillis = 1,
        )
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun init_rejectsNonPositiveScanWindows(): Unit {
    // Arrange
    val expectedMessage = "BleTransportConfig scanWindowMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        BleTransportConfig(
          scanIntervalMillis = 1,
          scanWindowMillis = 0,
          connectionTimeoutMillis = 1,
          advertisementIntervalMillis = 1,
        )
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun init_rejectsScanWindowsLargerThanTheInterval(): Unit {
    // Arrange
    val expectedMessage =
      "BleTransportConfig scanWindowMillis must be less than or equal to scanIntervalMillis."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        BleTransportConfig(
          scanIntervalMillis = 10,
          scanWindowMillis = 11,
          connectionTimeoutMillis = 1,
          advertisementIntervalMillis = 1,
        )
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun init_rejectsNonPositiveConnectionTimeouts(): Unit {
    // Arrange
    val expectedMessage = "BleTransportConfig connectionTimeoutMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        BleTransportConfig(
          scanIntervalMillis = 10,
          scanWindowMillis = 10,
          connectionTimeoutMillis = 0,
          advertisementIntervalMillis = 1,
        )
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun init_rejectsNonPositiveAdvertisementIntervals(): Unit {
    // Arrange
    val expectedMessage = "BleTransportConfig advertisementIntervalMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        BleTransportConfig(
          scanIntervalMillis = 10,
          scanWindowMillis = 10,
          connectionTimeoutMillis = 1,
          advertisementIntervalMillis = 0,
        )
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }
}
