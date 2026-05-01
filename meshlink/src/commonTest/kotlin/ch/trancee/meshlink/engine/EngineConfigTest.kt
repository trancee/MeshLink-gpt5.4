package ch.trancee.meshlink.engine

import ch.trancee.meshlink.api.MeshLinkConfig
import ch.trancee.meshlink.transport.BleTransportConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class EngineConfigTest {
  @Test
  public fun handshakeConfig_defaultExposesExpectedTimeoutRetryValues(): Unit {
    // Arrange
    // Act
    val actual = HandshakeConfig.default()

    // Assert
    assertEquals(expected = 5_000L, actual = actual.timeoutMillis)
    assertEquals(expected = 2, actual = actual.maxRetries)
    assertEquals(expected = 250L, actual = actual.initialRetryDelayMillis)
  }

  @Test
  public fun handshakeConfig_rejectsNonPositiveTimeouts(): Unit {
    // Arrange
    val expectedMessage = "HandshakeConfig timeoutMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        HandshakeConfig(timeoutMillis = 0L, maxRetries = 0, initialRetryDelayMillis = 1L)
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun handshakeConfig_rejectsNegativeRetryCounts(): Unit {
    // Arrange
    val expectedMessage = "HandshakeConfig maxRetries must be greater than or equal to 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        HandshakeConfig(timeoutMillis = 1L, maxRetries = -1, initialRetryDelayMillis = 1L)
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun handshakeConfig_rejectsNonPositiveInitialRetryDelays(): Unit {
    // Arrange
    val expectedMessage = "HandshakeConfig initialRetryDelayMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        HandshakeConfig(timeoutMillis = 1L, maxRetries = 0, initialRetryDelayMillis = 0L)
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }

  @Test
  public fun meshEngineConfig_defaultAggregatesSubsystemDefaults(): Unit {
    // Arrange
    // Act
    val actual = MeshEngineConfig.default()

    // Assert
    assertEquals(expected = MeshLinkConfig.default(), actual = actual.meshLinkConfig)
    assertEquals(expected = BleTransportConfig.default(), actual = actual.bleTransportConfig)
    assertEquals(expected = HandshakeConfig.default(), actual = actual.handshakeConfig)
    assertEquals(expected = 30_000L, actual = actual.sweepIntervalMillis)
  }

  @Test
  public fun meshEngineConfig_rejectsNonPositiveSweepIntervals(): Unit {
    // Arrange
    val expectedMessage = "MeshEngineConfig sweepIntervalMillis must be greater than 0."

    // Act
    val error =
      assertFailsWith<IllegalArgumentException> {
        MeshEngineConfig(
          meshLinkConfig = MeshLinkConfig.default(),
          bleTransportConfig = BleTransportConfig.default(),
          handshakeConfig = HandshakeConfig.default(),
          sweepIntervalMillis = 0L,
        )
      }

    // Assert
    assertEquals(expected = expectedMessage, actual = error.message)
  }
}
