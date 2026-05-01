import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.benchmark)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()
    jvmToolchain(21)

    jvm {
        compilations.create("benchmark") {
            associateWith(this@jvm.compilations.getByName("main"))
            defaultSourceSet.dependencies {
                implementation(libs.kotlinx.benchmark.runtime)
            }
        }
    }
    iosArm64()

    android {
        namespace = "ch.trancee.meshlink"
        compileSdk = 35
        minSdk = 29

        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("jvmTest").dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mockito.core)
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "MeshLink"
            isStatic = true
        }
    }
}

benchmark {
    targets {
        register("jvmBenchmark")
    }
    configurations {
        named("main") {
            include(".*WireFormatBenchmark.*")
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

kover {
    reports {
        filters {
            excludes {
                classes("ch.trancee.meshlink.wire.WireFormatBenchmark")
            }
        }
        variant("jvm") {
            html {
                title = "MeshLink JVM Coverage"
            }
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
            html {
                title = "MeshLink Android host-test Coverage"
            }
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
