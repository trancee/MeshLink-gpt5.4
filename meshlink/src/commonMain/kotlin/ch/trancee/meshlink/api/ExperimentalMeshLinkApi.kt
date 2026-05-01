package ch.trancee.meshlink.api

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This MeshLink API is experimental and may change without notice.",
)
public annotation class ExperimentalMeshLinkApi
