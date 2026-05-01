package ch.trancee.meshlink.wire

/** Result of structural frame validation. */
public sealed interface ValidationResult {
  /** Frame passed validation. */
  public data object Valid : ValidationResult

  /** Frame failed validation with a code and human-readable explanation. */
  public data class Invalid(public val code: ValidationFailureCode, public val reason: String) :
    ValidationResult
}
