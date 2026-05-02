// Settings script defines plugin/dependency repositories and the project graph.
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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
