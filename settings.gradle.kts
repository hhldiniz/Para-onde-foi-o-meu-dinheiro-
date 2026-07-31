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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The Kotlin Gradle plugin's wasmJs/js support needs this to download
        // the Node.js distribution used to run webpack; without it declared
        // here, FAIL_ON_PROJECT_REPOS rejects the plugin's own project-level
        // equivalent, and PREFER_SETTINGS just as bluntly ignores it, leaving
        // Gradle to search (and fail) in the Maven repos above instead. Exact
        // coordinates from the Kotlin Gradle plugin's own integration tests.
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
