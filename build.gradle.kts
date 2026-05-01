import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.benchmark) apply false
    alias(libs.plugins.kover) apply false
}

allprojects {
    group = "ch.trancee.meshlink"
    version = "0.1.0-SNAPSHOT"
}

tasks.wrapper {
    gradleVersion = "9.5.0"
    distributionType = Wrapper.DistributionType.ALL
}
