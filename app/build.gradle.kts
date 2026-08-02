@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaVersion.get()))
        }
    }

    // Device (arm64) and Apple-silicon simulator (arm64); both produce the same
    // `ComposeApp` framework consumed by the Xcode project in iosApp/. There is
    // no iosX64 target because Compose Multiplatform stopped publishing for the
    // Intel simulator.
    // Note: these can only be *compiled* on a macOS host; the Android target
    // (and therefore `testDebugUnitTest`) builds everywhere.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Compose Multiplatform for Web, deployed as a static site to GitHub
    // Pages. Room has no wasmJs target (see the `roomMain` source set below),
    // so this target gets a hand-rolled localStorage-backed persistence layer
    // instead (data/local/web/).
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "praondefoiomeudinheiro.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigation.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }

        // Room 2.7.1 (the version this project is pinned to) has no wasmJs/js
        // target — only the breaking-change Room 3.0 alpha line adds that, and
        // it needs a hand-rolled Web Worker + OPFS setup. So Room-touching code
        // (AppDatabase, the @Entity/@Dao classes) lives here instead of
        // commonMain, and only Android/iOS depend on it; wasmJs gets its own
        // Room-free persistence (see data/local/web/ under wasmJsMain).
        val roomMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
            }
        }

        androidMain.get().dependsOn(roomMain)
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.documentfile)
            implementation(libs.koin.android)
            implementation(libs.pdfbox.android)
            implementation(libs.mlkit.text.recognition)
        }

        iosMain.get().dependsOn(roomMain)

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockito.kotlin)
            implementation(libs.androidx.arch.core.testing)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.junit)
            implementation(libs.androidx.espresso.core)
            implementation(libs.pdfbox.android)
            // AppDatabaseTest drives suspending DAO calls with runTest. This used
            // to arrive transitively through compose ui-test-junit4, which these
            // tests no longer need.
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.hhldiniz.praondefoiomeudinheiro"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hhldiniz.praondefoiomeudinheiro"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // `optimization { }` is AGP 9 new-DSL only, which is disabled above.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
    }
    buildFeatures {
        compose = true
    }
}

compose.resources {
    // Strings live in commonMain/composeResources and are reached through the
    // generated `Res` class instead of Android's `R`, so the same lookups work
    // on both platforms.
    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
    packageOfResClass = "com.hhldiniz.praondefoiomeudinheiro.resources"
}

dependencies {
    // Room's compiler has to run once per Kotlin target that compiles the DAOs.
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)

    debugImplementation(compose.uiTooling)
}
