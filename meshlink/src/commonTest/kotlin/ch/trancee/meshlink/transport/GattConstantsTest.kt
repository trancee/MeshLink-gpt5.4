package ch.trancee.meshlink.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class GattConstantsTest {
  @Test
  public fun uuids_areDistinctAndCanonicalLength(): Unit {
    // Arrange
    val uuids =
      listOf(
        GattConstants.SERVICE_UUID,
        GattConstants.WRITE_CHARACTERISTIC_UUID,
        GattConstants.NOTIFY_CHARACTERISTIC_UUID,
        GattConstants.L2CAP_PSM_CHARACTERISTIC_UUID,
      )

    // Act
    val distinctCount = uuids.toSet().size
    val allCanonicalLength = uuids.all { uuid -> uuid.length == 36 }

    // Assert
    assertEquals(expected = uuids.size, actual = distinctCount)
    assertTrue(
      actual = allCanonicalLength,
      message = "GattConstants should expose canonical 128-bit UUID strings",
    )
  }

  @Test
  public fun uuids_useExpectedPrefixesForMeshLinkGattLayout(): Unit {
    // Arrange
    // Act
    val actualPrefixes =
      listOf(
        GattConstants.SERVICE_UUID.substring(startIndex = 0, endIndex = 8),
        GattConstants.WRITE_CHARACTERISTIC_UUID.substring(startIndex = 0, endIndex = 8),
        GattConstants.NOTIFY_CHARACTERISTIC_UUID.substring(startIndex = 0, endIndex = 8),
        GattConstants.L2CAP_PSM_CHARACTERISTIC_UUID.substring(startIndex = 0, endIndex = 8),
      )

    // Assert
    assertEquals(
      expected = listOf("c0de0001", "c0de0002", "c0de0003", "c0de0004"),
      actual = actualPrefixes,
    )
  }
}
