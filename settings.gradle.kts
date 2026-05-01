// Settings script defines plugin/dependency repositories and the project graph. It also
// contains a temporary version-catalog parsing workaround for buildscript constraints.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

buildscript {
    val versionCatalogText: String = settingsDir.resolve("gradle/libs.versions.toml").readText()

    // buildscript{} cannot use the generated version-catalog accessors yet, so we read the
    // TOML directly to keep the forced classpath versions aligned with libs.versions.toml.
    fun versionFromCatalog(name: String): String {
        val match = Regex("^${name}\\s*=\\s*\"([^\"]+)\"$", RegexOption.MULTILINE).find(versionCatalogText)
        return match?.groupValues?.get(1) ?: error("Missing version '$name' in gradle/libs.versions.toml")
    }

    val bouncycastleVersion = versionFromCatalog("bouncycastle")
    val jose4jVersion = versionFromCatalog("jose4j")
    val jdom2Version = versionFromCatalog("jdom2")
    val commonsLang3Version = versionFromCatalog("commonsLang3")
    val jacksonVersion = versionFromCatalog("jackson")
    val httpclientVersion = versionFromCatalog("httpclient")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    configurations.classpath {
        resolutionStrategy.force(
            "org.bouncycastle:bcpg-jdk18on:$bouncycastleVersion",
            "org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion",
            "org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion",
            "org.bouncycastle:bcutil-jdk18on:$bouncycastleVersion",
            "org.bitbucket.b_c:jose4j:$jose4jVersion",
            "org.jdom:jdom2:$jdom2Version",
            "org.apache.commons:commons-lang3:$commonsLang3Version",
            "org.apache.httpcomponents:httpclient:$httpclientVersion",
            "com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion",
            "com.fasterxml.jackson.core:jackson-core:$jacksonVersion",
            "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion",
            "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion",
        )
    }
    dependencies {
        constraints {
            classpath("org.bouncycastle:bcpg-jdk18on:$bouncycastleVersion")
            classpath("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")
            classpath("org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion")
            classpath("org.bouncycastle:bcutil-jdk18on:$bouncycastleVersion")
            classpath("org.bitbucket.b_c:jose4j:$jose4jVersion")
            classpath("org.jdom:jdom2:$jdom2Version")
            classpath("org.apache.commons:commons-lang3:$commonsLang3Version")
            classpath("org.apache.httpcomponents:httpclient:$httpclientVersion")
            classpath("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
            classpath("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
            classpath("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
            classpath("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
            classpath("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        }
    }
}

// Enforce repository declaration at the settings level so subprojects do not drift in
// dependency resolution behavior.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MeshLink"
include(":meshlink")
