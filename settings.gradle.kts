pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS doesn't work here even with the settings-declared
    // Ivy repo below: the Kotlin Gradle plugin's wasmJs/js support still adds
    // its own project-level repo of the same kind (under a different name,
    // so Gradle doesn't treat it as already covered), and FAIL_ON_PROJECT_REPOS
    // rejects any project-level repository unconditionally. PREFER_SETTINGS
    // is the documented way to combine centralized repository management
    // with a wasmJs/js target; declaring the repo below is still what makes
    // dependency resolution actually succeed under it, rather than silently
    // ignoring the plugin's repo and failing to find org.nodejs:node in the
    // Maven repos beneath.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // The Node.js distribution used to run webpack for the wasmJs
        // target; exact coordinates from the Kotlin Gradle plugin's own
        // integration tests.
        ivy("https://nodejs.org/dist") {
            name = "Node Distributions at https://nodejs.org/dist"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
    }
}

rootProject.name = "Pra onde foi o meu dinheiro"
include(":app")
