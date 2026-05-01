package ch.trancee.meshlink.power

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

public class PowerManagerTest {
    @Test
    public fun evaluate_selectsHighNormalAndLowTiersAtTheConfiguredThresholds(): Unit {
        // Arrange
        val batteryMonitor = FixedBatteryMonitor(initialBatteryPercent = 80)
        val powerManager = PowerManager(
            batteryMonitor = batteryMonitor,
            config = PowerConfig.default(),
        )

        // Act
        val high = powerManager.evaluate(connections = emptyList())
        batteryMonitor.update(batteryPercent = 50)
        val normal = powerManager.evaluate(connections = emptyList())
        batteryMonitor.update(batteryPercent = 10)
        val low = powerManager.evaluate(connections = emptyList())

        // Assert
        assertEquals(expected = PowerTier.HIGH, actual = high.tier)
        assertEquals(expected = PowerTier.NORMAL, actual = normal.tier)
        assertEquals(expected = PowerTier.LOW, actual = low.tier)
    }

    @Test
    public fun evaluate_appliesHysteresisToTierTransitions(): Unit {
        // Arrange
        val config = PowerConfig.default()
        val batteryMonitor = FixedBatteryMonitor(initialBatteryPercent = 80)
        val powerManager = PowerManager(
            batteryMonitor = batteryMonitor,
            config = config,
        )
        powerManager.evaluate(connections = emptyList())

        // Act
        batteryMonitor.update(batteryPercent = 78)
        val stillHigh = powerManager.evaluate(connections = emptyList())
        batteryMonitor.update(batteryPercent = 74)
        val downgraded = powerManager.evaluate(connections = emptyList())
        batteryMonitor.update(batteryPercent = 84)
        val stillNormal = powerManager.evaluate(connections = emptyList())
        batteryMonitor.update(batteryPercent = 85)
        val promotedToHigh = powerManager.evaluate(connections = emptyList())
        val lowBatteryMonitor = FixedBatteryMonitor(initialBatteryPercent = 10)
        val promotedFromLowManager = PowerManager(
            batteryMonitor = lowBatteryMonitor,
            config = config,
        )
        promotedFromLowManager.evaluate(connections = emptyList())
        lowBatteryMonitor.update(batteryPercent = 36)
        val promotedFromLow = promotedFromLowManager.evaluate(connections = emptyList())
        lowBatteryMonitor.update(batteryPercent = 34)
        val stillLow = promotedFromLowManager.evaluate(connections = emptyList())

        // Assert
        assertEquals(expected = PowerTier.HIGH, actual = stillHigh.tier)
        assertEquals(expected = PowerTier.NORMAL, actual = downgraded.tier)
        assertEquals(expected = PowerTier.NORMAL, actual = stillNormal.tier)
        assertEquals(expected = PowerTier.HIGH, actual = promotedToHigh.tier)
        assertEquals(expected = PowerTier.NORMAL, actual = promotedFromLow.tier)
        assertEquals(expected = PowerTier.NORMAL, actual = stillLow.tier)
    }

    @Test
    public fun evaluate_returnsProfilesConnectionAdmissionAndShedRecommendations(): Unit {
        // Arrange
        val batteryMonitor = FixedBatteryMonitor(initialBatteryPercent = 10)
        val powerManager = PowerManager(
            batteryMonitor = batteryMonitor,
            config = PowerConfig.default(),
        )
        val connections = listOf(
            ManagedConnection(peerKey = PeerKey(value = "a"), transferStatus = TransferStatus.IN_FLIGHT, lastActivityEpochMillis = 30L),
            ManagedConnection(peerKey = PeerKey(value = "b"), transferStatus = TransferStatus.COMPLETE, lastActivityEpochMillis = 10L),
            ManagedConnection(peerKey = PeerKey(value = "c"), transferStatus = TransferStatus.IDLE, lastActivityEpochMillis = 20L),
        )

        // Act
        val actual = powerManager.evaluate(connections = connections)

        // Assert
        assertEquals(expected = PowerTier.LOW, actual = actual.tier)
        assertEquals(expected = PowerConfig.default().lowProfile, actual = actual.profile)
        assertEquals(expected = false, actual = actual.canAcceptConnection)
        assertEquals(expected = listOf(connections[1]), actual = actual.shedConnections)
    }

    @Test
    public fun policyHelpers_coverAllTierAndSheddingBranches(): Unit {
        // Arrange
        val config = PowerConfig.default()
        val highConnections = listOf(
            ManagedConnection(peerKey = PeerKey(value = "in-flight"), transferStatus = TransferStatus.IN_FLIGHT, lastActivityEpochMillis = 30L),
            ManagedConnection(peerKey = PeerKey(value = "idle"), transferStatus = TransferStatus.IDLE, lastActivityEpochMillis = 20L),
            ManagedConnection(peerKey = PeerKey(value = "complete"), transferStatus = TransferStatus.COMPLETE, lastActivityEpochMillis = 10L),
        )

        // Act
        val highProfile = BleConnectionParameterPolicy.profileFor(tier = PowerTier.HIGH, config = config)
        val normalProfile = BleConnectionParameterPolicy.profileFor(tier = PowerTier.NORMAL, config = config)
        val lowProfile = BleConnectionParameterPolicy.profileFor(tier = PowerTier.LOW, config = config)
        val canAcceptHigh = ConnectionLimiter.canAcceptConnection(currentConnectionCount = 7, tier = PowerTier.HIGH, config = config)
        val cannotAcceptLow = ConnectionLimiter.canAcceptConnection(currentConnectionCount = 2, tier = PowerTier.LOW, config = config)
        val noShedNeeded = TieredShedder.shed(connections = highConnections, targetConnectionCount = 3)
        val shedTwo = TieredShedder.shed(connections = highConnections, targetConnectionCount = 1)
        val shedInFlight = TieredShedder.shed(
            connections = listOf(
                ManagedConnection(peerKey = PeerKey(value = "only"), transferStatus = TransferStatus.IN_FLIGHT, lastActivityEpochMillis = 5L),
            ),
            targetConnectionCount = 0,
        )
        val lowBatteryMonitor = FixedBatteryMonitor(initialBatteryPercent = 10)
        val lowManager = PowerManager(batteryMonitor = lowBatteryMonitor, config = config)
        lowManager.evaluate(connections = emptyList())
        lowBatteryMonitor.update(batteryPercent = 34)
        val stillLow = lowManager.evaluate(connections = emptyList())

        // Assert
        assertEquals(expected = config.highProfile, actual = highProfile)
        assertEquals(expected = config.normalProfile, actual = normalProfile)
        assertEquals(expected = config.lowProfile, actual = lowProfile)
        assertEquals(expected = true, actual = canAcceptHigh)
        assertEquals(expected = false, actual = cannotAcceptLow)
        assertEquals(expected = emptyList(), actual = noShedNeeded)
        assertEquals(expected = listOf(highConnections[2], highConnections[1]), actual = shedTwo)
        assertEquals(
            expected = listOf(
                ManagedConnection(peerKey = PeerKey(value = "only"), transferStatus = TransferStatus.IN_FLIGHT, lastActivityEpochMillis = 5L),
            ),
            actual = shedInFlight,
        )
        assertEquals(expected = PowerTier.LOW, actual = stillLow.tier)
    }

    @Test
    public fun helperPoliciesAndTypes_rejectInvalidInputs(): Unit {
        // Arrange
        // Act
        val hysteresisError = assertFailsWith<IllegalArgumentException> {
            PowerModeEngine(hysteresisPercent = -1)
        }
        val batteryPercentError = assertFailsWith<IllegalArgumentException> {
            PowerModeEngine().evaluate(currentTier = null, batteryPercent = 101, config = PowerConfig.default())
        }
        val negativeBatteryPercentError = assertFailsWith<IllegalArgumentException> {
            PowerModeEngine().evaluate(currentTier = null, batteryPercent = -1, config = PowerConfig.default())
        }
        val connectionCountError = assertFailsWith<IllegalArgumentException> {
            ConnectionLimiter.canAcceptConnection(currentConnectionCount = -1, tier = PowerTier.HIGH, config = PowerConfig.default())
        }
        val targetCountError = assertFailsWith<IllegalArgumentException> {
            TieredShedder.shed(connections = emptyList(), targetConnectionCount = -1)
        }
        val peerKeyError = assertFailsWith<IllegalArgumentException> {
            PeerKey(value = "   ")
        }
        val connectionError = assertFailsWith<IllegalArgumentException> {
            ManagedConnection(peerKey = PeerKey(value = "peer"), transferStatus = TransferStatus.IDLE, lastActivityEpochMillis = -1L)
        }

        // Assert
        assertEquals(expected = "PowerModeEngine hysteresisPercent must be greater than or equal to 0.", actual = hysteresisError.message)
        assertEquals(expected = "PowerModeEngine batteryPercent must be between 0 and 100.", actual = batteryPercentError.message)
        assertEquals(expected = "PowerModeEngine batteryPercent must be between 0 and 100.", actual = negativeBatteryPercentError.message)
        assertEquals(expected = "ConnectionLimiter currentConnectionCount must be greater than or equal to 0.", actual = connectionCountError.message)
        assertEquals(expected = "TieredShedder targetConnectionCount must be greater than or equal to 0.", actual = targetCountError.message)
        assertEquals(expected = "PeerKey value must not be blank.", actual = peerKeyError.message)
        assertEquals(expected = "ManagedConnection lastActivityEpochMillis must be greater than or equal to 0.", actual = connectionError.message)
    }
}
