package ch.trancee.meshlink.power

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class FixedBatteryMonitorTest {
  @Test
  public fun batteryPercent_returnsTheCurrentBatteryLevelAndSupportsUpdates(): Unit {
    // Arrange
    val monitor = FixedBatteryMonitor(initialBatteryPercent = 80)

    // Act
    val initial = monitor.batteryPercent()
    monitor.update(batteryPercent = 25)
    val updated = monitor.batteryPercent()

    // Assert
    assertEquals(expected = 80, actual = initial)
    assertEquals(expected = 25, actual = updated)
  }

  @Test
  public fun constructionAndUpdates_rejectValuesOutsideTheBatteryRange(): Unit {
    // Arrange
    // Act
    val initError =
      assertFailsWith<IllegalArgumentException> { FixedBatteryMonitor(initialBatteryPercent = 101) }
    val monitor = FixedBatteryMonitor(initialBatteryPercent = 50)
    val updateError =
      assertFailsWith<IllegalArgumentException> { monitor.update(batteryPercent = -1) }

    // Assert
    assertEquals(
      expected = "FixedBatteryMonitor batteryPercent must be between 0 and 100.",
      actual = initError.message,
    )
    assertEquals(
      expected = "FixedBatteryMonitor batteryPercent must be between 0 and 100.",
      actual = updateError.message,
    )
  }
}
