import org.gradle.api.tasks.wrapper.Wrapper

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

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
        strictValidation = true
    }
}

tasks.wrapper {
    gradleVersion = "9.5.0"
    distributionType = Wrapper.DistributionType.ALL
}
