import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.benchmark)
  alias(libs.plugins.kover)
  alias(libs.plugins.detekt)
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka)
  alias(libs.plugins.skie)
  id("maven-publish")
  id("signing")
}

val meshLinkXCFramework = XCFramework("MeshLink")

kotlin {
  explicitApi()
  jvmToolchain(21)

  jvm {
    compilations.create("benchmark") {
      associateWith(this@jvm.compilations.getByName("main"))
      defaultSourceSet.dependencies { implementation(libs.kotlinx.benchmark.runtime) }
    }
  }
  iosArm64()

  android {
    namespace = "ch.trancee.meshlink"
    compileSdk = 35
    minSdk = 29

    withHostTest {}

    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
  }

  sourceSets {
    commonMain.dependencies { implementation(libs.kotlinx.coroutines.core) }
    commonTest.dependencies { implementation(kotlin("test")) }
    getByName("jvmTest").dependencies { implementation(libs.kotlinx.serialization.json) }
    getByName("androidHostTest").dependencies { implementation(libs.mockito.core) }
  }

  targets.withType<KotlinNativeTarget>().configureEach {
    binaries.framework {
      baseName = "MeshLink"
      isStatic = true
      binaryOption("bundleId", "ch.trancee.meshlink")
      binaryOption("bundleShortVersionString", project.version.toString())
      binaryOption("bundleVersion", "1")
      meshLinkXCFramework.add(this)
    }
  }
}

benchmark {
  targets { register("jvmBenchmark") }
  configurations {
    named("main") {
      include(".*(WireFormatBenchmark|RoutingBenchmark|DedupBenchmark|TransferBenchmark).*")
      warmups = 1
      iterations = 3
      iterationTime = 100
      iterationTimeUnit = "ms"
      mode = "avgt"
      outputTimeUnit = "us"
      reportFormat = "text"
    }
  }
}

tasks.register("jvmBenchmark") {
  group = "verification"
  description = "Runs the JVM benchmark suite."
  dependsOn("jvmBenchmarkBenchmark")
}

tasks.register("jvmCiBenchmark") {
  group = "verification"
  description = "Runs the CI-shortened JVM benchmark suite."
  dependsOn("jvmBenchmarkBenchmark")
}

detekt {
  buildUponDefaultConfig = true
  parallel = true
  basePath = rootDir.absolutePath
  config.setFrom(rootProject.file("detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  jvmTarget = JvmTarget.JVM_17.target
}

tasks.named("detekt") {
  dependsOn(
    "detektMetadataCommonMain",
    "detektJvmMain",
    "detektJvmTest",
    "detektJvmBenchmark",
    "detektAndroidMain",
    "detektAndroidHostTest",
    "detektIosArm64Main",
  )
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktfmt().googleStyle()
  }
  kotlinGradle {
    target("build.gradle.kts")
    ktfmt().googleStyle()
  }
}

tasks.register("ktfmtCheck") {
  group = "formatting"
  description = "Checks Kotlin source formatting via ktfmt."
  dependsOn("spotlessCheck")
}

tasks.register("ktfmtFormat") {
  group = "formatting"
  description = "Formats Kotlin sources via ktfmt."
  dependsOn("spotlessApply")
}

val javadocJar by
  tasks.registering(Jar::class) {
    group = "documentation"
    description = "Assembles the Dokka HTML publication as a javadoc jar."
    archiveClassifier.set("javadoc")
    dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
    from(tasks.named("dokkaGeneratePublicationHtml"))
  }

publishing {
  repositories {
    maven {
      name = "OSSRH"
      val isSnapshotVersion: Boolean = version.toString().endsWith("SNAPSHOT")
      url =
        uri(
          if (isSnapshotVersion) {
            "https://s01.oss.sonatype.org/content/repositories/snapshots/"
          } else {
            "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
          }
        )
      credentials {
        username = providers.environmentVariable("OSSRH_USERNAME").orNull
        password = providers.environmentVariable("OSSRH_PASSWORD").orNull
      }
    }
  }
  publications.withType<MavenPublication>().configureEach {
    artifact(javadocJar)
    pom {
      name.set("MeshLink")
      description.set("Kotlin Multiplatform mesh networking toolkit.")
      url.set("https://github.com/trancee/meshlink")
      licenses {
        license {
          name.set("Apache License, Version 2.0")
          url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
      }
      developers {
        developer {
          id.set("meshlink")
          name.set("MeshLink Maintainers")
        }
      }
      scm {
        url.set("https://github.com/trancee/meshlink")
        connection.set("scm:git:https://github.com/trancee/meshlink.git")
        developerConnection.set("scm:git:ssh://git@github.com/trancee/meshlink.git")
      }
    }
  }
}

signing {
  val signingKey: String? = providers.environmentVariable("SIGNING_KEY").orNull
  val signingPassword: String? = providers.environmentVariable("SIGNING_PASSWORD").orNull
  val hasSigningCredentials: Boolean =
    !signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()

  isRequired = hasSigningCredentials && !version.toString().endsWith("SNAPSHOT")

  if (hasSigningCredentials) {
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
  }
}

kover {
  reports {
    filters {
      excludes {
        classes(
          "ch.trancee.meshlink.api.PeerIdHex",
          "ch.trancee.meshlink.routing.DedupBenchmark",
          "ch.trancee.meshlink.routing.RoutingBenchmark",
          "ch.trancee.meshlink.transfer.TransferBenchmark",
          "ch.trancee.meshlink.wire.WireFormatBenchmark",
        )
      }
    }
    variant("jvm") {
      html { title = "MeshLink JVM Coverage" }
      log {
        header = "MeshLink JVM coverage"
        groupBy = GroupingEntityType.APPLICATION
        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
        coverageUnits = CoverageUnit.LINE
        format = "<entity> line coverage: <value>%"
      }
      verify {
        rule("100% line coverage") {
          bound {
            minValue = 100
            coverageUnits = CoverageUnit.LINE
            aggregationForGroup = AggregationType.COVERED_PERCENTAGE
          }
        }
        rule("100% branch coverage") {
          bound {
            minValue = 100
            coverageUnits = CoverageUnit.BRANCH
            aggregationForGroup = AggregationType.COVERED_PERCENTAGE
          }
        }
      }
    }
    variant("android") {
      html { title = "MeshLink Android host-test Coverage" }
      log {
        header = "MeshLink Android host-test coverage"
        groupBy = GroupingEntityType.APPLICATION
        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
        coverageUnits = CoverageUnit.LINE
        format = "<entity> line coverage: <value>%"
      }
      verify {
        rule("100% line coverage") {
          bound {
            minValue = 100
            coverageUnits = CoverageUnit.LINE
            aggregationForGroup = AggregationType.COVERED_PERCENTAGE
          }
        }
        rule("100% branch coverage") {
          bound {
            minValue = 100
            coverageUnits = CoverageUnit.BRANCH
            aggregationForGroup = AggregationType.COVERED_PERCENTAGE
          }
        }
      }
    }
  }
}
