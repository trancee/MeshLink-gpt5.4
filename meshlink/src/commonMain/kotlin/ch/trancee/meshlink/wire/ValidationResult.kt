package ch.trancee.meshlink.wire

public sealed interface ValidationResult {
  public data object Valid : ValidationResult

  public data class Invalid(public val code: ValidationFailureCode, public val reason: String) :
    ValidationResult
}
