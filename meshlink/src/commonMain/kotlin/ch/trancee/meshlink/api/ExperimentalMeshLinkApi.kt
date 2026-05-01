package ch.trancee.meshlink.api

/** Marks MeshLink APIs that are not yet considered stable. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
  level = RequiresOptIn.Level.WARNING,
  message = "This MeshLink API is experimental and may change without notice.",
)
public annotation class ExperimentalMeshLinkApi
