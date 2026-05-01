package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class DiagnosticCodeTest {
    @Test
    public fun entries_coverTwentySixDiagnosticCodes(): Unit {
        // Arrange
        val expectedCount: Int = 26

        // Act
        val actualCount: Int = DiagnosticCode.entries.size

        // Assert
        assertEquals(
            expected = expectedCount,
            actual = actualCount,
            message = "DiagnosticCode should expose the canonical 26 diagnostic codes",
        )
    }

    @Test
    public fun severities_spanAllFourDiagnosticLevels(): Unit {
        // Arrange
        val expectedLevels: Set<DiagnosticSeverity> = setOf(
            DiagnosticSeverity.DEBUG,
            DiagnosticSeverity.INFO,
            DiagnosticSeverity.WARN,
            DiagnosticSeverity.ERROR,
        )

        // Act
        val actualLevels: Set<DiagnosticSeverity> = DiagnosticCode.entries.map { code -> code.severity }.toSet()

        // Assert
        assertEquals(
            expected = expectedLevels,
            actual = actualLevels,
            message = "DiagnosticCode should cover all four diagnostic severity levels",
        )
    }

    @Test
    public fun errorCodes_arePresentForFailureScenarios(): Unit {
        // Arrange
        val errorCodes = setOf(
            DiagnosticCode.HANDSHAKE_FAILED,
            DiagnosticCode.MESSAGE_FAILED,
            DiagnosticCode.TRANSFER_FAILED,
            DiagnosticCode.INTERNAL_ERROR,
        )

        // Act
        val actual = DiagnosticCode.entries.filter { code -> code.severity == DiagnosticSeverity.ERROR }.toSet()

        // Assert
        assertTrue(
            actual = actual.containsAll(errorCodes),
            message = "DiagnosticCode should include explicit ERROR codes for handshake, messaging, transfer, and internal failures",
        )
    }
}
