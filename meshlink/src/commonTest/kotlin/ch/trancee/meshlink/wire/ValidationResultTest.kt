package ch.trancee.meshlink.wire

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class ValidationResultTest {
    @Test
    public fun valid_representsSuccessfulValidation(): Unit {
        // Arrange
        val result: ValidationResult = ValidationResult.Valid

        // Act
        val actualIsValid: Boolean = result === ValidationResult.Valid

        // Assert
        assertTrue(
            actual = actualIsValid,
            message = "ValidationResult.Valid should be the singleton successful validation result",
        )
    }

    @Test
    public fun invalid_retainsFailureCodeAndReason(): Unit {
        // Arrange
        val expectedCode: ValidationFailureCode = ValidationFailureCode.PAYLOAD_EXCEEDS_MAX_SIZE
        val expectedReason: String = "payload too large"

        // Act
        val actual = ValidationResult.Invalid(code = expectedCode, reason = expectedReason)

        // Assert
        assertEquals(
            expected = expectedCode,
            actual = actual.code,
            message = "ValidationResult.Invalid should retain the failure code",
        )
        assertEquals(
            expected = expectedReason,
            actual = actual.reason,
            message = "ValidationResult.Invalid should retain the failure reason",
        )
    }
}
