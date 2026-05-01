package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class MeshLinkStateTest {
    @Test
    public fun uninitialized_canTransitionToRunning(): Unit {
        // Arrange
        val current = MeshLinkState.UNINITIALIZED

        // Act
        val actual: Boolean = current.canTransitionTo(target = MeshLinkState.RUNNING)

        // Assert
        assertTrue(
            actual = actual,
            message = "MeshLinkState should allow UNINITIALIZED to transition to RUNNING",
        )
    }

    @Test
    public fun running_canTransitionToPausedRecoveredStoppedAndTerminal(): Unit {
        // Arrange
        val current = MeshLinkState.RUNNING

        // Act
        val canPause: Boolean = current.canTransitionTo(target = MeshLinkState.PAUSED)
        val canRecover: Boolean = current.canTransitionTo(target = MeshLinkState.RECOVERABLE)
        val canStop: Boolean = current.canTransitionTo(target = MeshLinkState.STOPPED)
        val canTerminate: Boolean = current.canTransitionTo(target = MeshLinkState.TERMINAL)

        // Assert
        assertTrue(actual = canPause)
        assertTrue(actual = canRecover)
        assertTrue(actual = canStop)
        assertTrue(actual = canTerminate)
    }

    @Test
    public fun paused_canResumeOrStop(): Unit {
        // Arrange
        val current = MeshLinkState.PAUSED

        // Act
        val canResume: Boolean = current.canTransitionTo(target = MeshLinkState.RUNNING)
        val canStop: Boolean = current.canTransitionTo(target = MeshLinkState.STOPPED)

        // Assert
        assertTrue(actual = canResume)
        assertTrue(actual = canStop)
    }

    @Test
    public fun stopped_canRestart(): Unit {
        // Arrange
        val current = MeshLinkState.STOPPED

        // Act
        val actual: Boolean = current.canTransitionTo(target = MeshLinkState.RUNNING)

        // Assert
        assertTrue(
            actual = actual,
            message = "MeshLinkState should allow STOPPED to restart into RUNNING",
        )
    }

    @Test
    public fun recoverable_canReturnToRunningOrStop(): Unit {
        // Arrange
        val current = MeshLinkState.RECOVERABLE

        // Act
        val canReturnToRunning: Boolean = current.canTransitionTo(target = MeshLinkState.RUNNING)
        val canStop: Boolean = current.canTransitionTo(target = MeshLinkState.STOPPED)

        // Assert
        assertTrue(actual = canReturnToRunning)
        assertTrue(actual = canStop)
    }

    @Test
    public fun terminal_cannotTransitionAnywhere(): Unit {
        // Arrange
        val current = MeshLinkState.TERMINAL

        // Act
        val actual: Boolean = current.canTransitionTo(target = MeshLinkState.RUNNING)

        // Assert
        assertFalse(
            actual = actual,
            message = "MeshLinkState should make TERMINAL a sink state",
        )
    }

    @Test
    public fun invalidTransitions_areRejected(): Unit {
        // Arrange
        val uninitialized = MeshLinkState.UNINITIALIZED
        val paused = MeshLinkState.PAUSED

        // Act
        val uninitializedToPaused: Boolean = uninitialized.canTransitionTo(target = MeshLinkState.PAUSED)
        val pausedToRecoverable: Boolean = paused.canTransitionTo(target = MeshLinkState.RECOVERABLE)

        // Assert
        assertFalse(
            actual = uninitializedToPaused,
            message = "MeshLinkState should reject jumping directly from UNINITIALIZED to PAUSED",
        )
        assertFalse(
            actual = pausedToRecoverable,
            message = "MeshLinkState should reject PAUSED to RECOVERABLE transitions",
        )
    }

    @Test
    public fun canTransitionTo_matchesTheFullStateMatrix(): Unit {
        // Arrange
        val expectedTargetsByState: Map<MeshLinkState, Set<MeshLinkState>> = mapOf(
            MeshLinkState.UNINITIALIZED to setOf(MeshLinkState.RUNNING, MeshLinkState.TERMINAL),
            MeshLinkState.RUNNING to setOf(MeshLinkState.PAUSED, MeshLinkState.STOPPED, MeshLinkState.RECOVERABLE, MeshLinkState.TERMINAL),
            MeshLinkState.PAUSED to setOf(MeshLinkState.RUNNING, MeshLinkState.STOPPED, MeshLinkState.TERMINAL),
            MeshLinkState.STOPPED to setOf(MeshLinkState.RUNNING, MeshLinkState.TERMINAL),
            MeshLinkState.RECOVERABLE to setOf(MeshLinkState.RUNNING, MeshLinkState.STOPPED, MeshLinkState.TERMINAL),
            MeshLinkState.TERMINAL to emptySet(),
        )

        // Act / Assert
        expectedTargetsByState.forEach { (from, expectedTargets) ->
            MeshLinkState.entries.forEach { target ->
                assertEquals(
                    expected = target in expectedTargets,
                    actual = from.canTransitionTo(target = target),
                    message = "MeshLinkState should follow the declared transition matrix for $from -> $target",
                )
            }
        }
    }
}
