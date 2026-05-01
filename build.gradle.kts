import org.gradle.api.tasks.wrapper.Wrapper

// Root build script that centralizes plugin availability, dependency constraints used by
// publishing/signing plugins, and repository-wide metadata shared by all modules.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.benchmark) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

// These constraints keep transitive buildscript dependencies on a known-good set of
// versions, which avoids plugin classpath drift between local and CI builds.
buildscript {
    dependencies {
        constraints {
            classpath("org.bouncycastle:bcpg-jdk18on:${libs.versions.bouncycastle.get()}")
            classpath("org.bouncycastle:bcpkix-jdk18on:${libs.versions.bouncycastle.get()}")
            classpath("org.bouncycastle:bcprov-jdk18on:${libs.versions.bouncycastle.get()}")
            classpath("org.bouncycastle:bcutil-jdk18on:${libs.versions.bouncycastle.get()}")
            classpath("org.bitbucket.b_c:jose4j:${libs.versions.jose4j.get()}")
            classpath("org.jdom:jdom2:${libs.versions.jdom2.get()}")
            classpath("org.apache.commons:commons-lang3:${libs.versions.commonsLang3.get()}")
            classpath("org.apache.httpcomponents:httpclient:${libs.versions.httpclient.get()}")
            classpath("com.fasterxml.jackson.core:jackson-annotations:${libs.versions.jackson.get()}")
            classpath("com.fasterxml.jackson.core:jackson-core:${libs.versions.jackson.get()}")
            classpath("com.fasterxml.jackson.core:jackson-databind:${libs.versions.jackson.get()}")
            classpath("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${libs.versions.jackson.get()}")
            classpath("com.fasterxml.jackson.module:jackson-module-kotlin:${libs.versions.jackson.get()}")
        }
    }
}

allprojects {
    group = "ch.trancee.meshlink"
    version = "0.1.0-SNAPSHOT"
}

// Enable strict JVM and KLib API validation so accidental public-surface changes are
// caught during development rather than after publishing.
apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
        strictValidation = true
    }
}

// Ship the full wrapper distribution so IDEs and Dokka-related workflows have source
// and documentation artifacts available without extra downloads.
tasks.wrapper {
    gradleVersion = "9.5.0"
    distributionType = Wrapper.DistributionType.ALL
}
