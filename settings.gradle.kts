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
    // PREFER_SETTINGS rather than FAIL_ON_PROJECT_REPOS: the Kotlin Gradle
    // plugin's wasmJs/js support always registers its own project-level Ivy
    // repository for downloading the Node.js distribution
    // (https://nodejs.org/dist), which FAIL_ON_PROJECT_REPOS hard-rejects
    // regardless of who added it.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Pra onde foi o meu dinheiro"
include(":app")
