package ch.trancee.meshlink.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class ExperimentalMeshLinkApiTest {
  @Test
  public fun annotation_declaresBinaryRetentionAndExpectedTargets(): Unit {
    // Arrange
    val annotationClass = ExperimentalMeshLinkApi::class.java
    val retention: Retention = requireNotNull(annotationClass.getAnnotation(Retention::class.java))
    val target: Target = requireNotNull(annotationClass.getAnnotation(Target::class.java))

    // Act
    val actualRetention: AnnotationRetention = retention.value
    val actualTargets: Set<AnnotationTarget> = target.allowedTargets.toSet()

    // Assert
    assertEquals(
      expected = AnnotationRetention.BINARY,
      actual = actualRetention,
      message = "ExperimentalMeshLinkApi should use binary retention to avoid runtime overhead",
    )
    assertTrue(actual = AnnotationTarget.CLASS in actualTargets)
    assertTrue(actual = AnnotationTarget.FUNCTION in actualTargets)
    assertTrue(actual = AnnotationTarget.PROPERTY in actualTargets)
  }
}
