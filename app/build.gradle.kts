@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import java.util.Properties
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

        // The assistant chat's on-device model runtime for Android and iOS:
        // llama.cpp through Llamatik, plus the Ktor client that downloads the
        // GGUF file. The web build runs llama.cpp through a vendored wllama
        // instead (Llamatik's own wasm bridge hardcodes a root-relative worker
        // path that cannot work from the GitHub Pages subdirectory), so — like
        // Room — this lives in its own intermediate source set that wasmJs
        // does not depend on.
        val llamaMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.llamatik)
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.io.core)
            }
        }

        androidMain.get().dependsOn(roomMain)
        androidMain.get().dependsOn(llamaMain)
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.documentfile)
            implementation(libs.koin.android)
            implementation(libs.pdfbox.android)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.ktor.client.android)
        }

        iosMain.get().dependsOn(roomMain)
        iosMain.get().dependsOn(llamaMain)
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

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

// Release signing material. In CI it arrives as environment variables (see
// .github/workflows/release.yml, which either decodes a keystore from the
// repository secrets or generates a self-signed one with
// scripts/generate-release-keystore.sh); locally it comes from an untracked
// keystore.properties at the repository root. Both are read through the
// `providers` API so the configuration cache knows they are build inputs.
val keystoreProperties = Properties().apply {
    providers
        .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText
        .orNull
        ?.let { load(it.reader()) }
}

val signingSetting: (String, String) -> String? = { property, environmentVariable ->
    (providers.environmentVariable(environmentVariable).orNull ?: keystoreProperties.getProperty(property))
        ?.takeIf { it.isNotBlank() }
}

val releaseStoreFile = signingSetting("storeFile", "RELEASE_KEYSTORE_FILE")
val releaseStorePassword = signingSetting("storePassword", "RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingSetting("keyAlias", "RELEASE_KEY_ALIAS")
// PKCS12 (what the script generates) cannot hold a key password of its own;
// a JKS keystore that does can still say so.
val releaseKeyPassword = signingSetting("keyPassword", "RELEASE_KEY_PASSWORD") ?: releaseStorePassword

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

    signingConfigs {
        // Only declared when there is something to sign with, so a checkout
        // with no keystore still configures (and `assembleRelease` still runs,
        // producing app-release-unsigned.apk).
        val storePath = releaseStoreFile
        val storePassphrase = releaseStorePassword
        val alias = releaseKeyAlias
        if (storePath != null && storePassphrase != null && alias != null) {
            create("release") {
                // Absolute paths pass through untouched; a relative one is
                // read from the repository root, which is where both
                // keystore.properties and the generated key live.
                storeFile = rootProject.file(storePath)
                storePassword = storePassphrase
                keyAlias = alias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // `optimization { }` is AGP 9 new-DSL only, which is disabled above.
            isMinifyEnabled = false
            // Null when nothing is configured: the build then produces an
            // unsigned APK rather than failing, which is the right outcome for
            // a contributor who only wants to check that release compiles.
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            // Llamatik ships llama.cpp for four ABIs, ~35MB each, stored
            // uncompressed in the APK. A 32-bit device cannot run the
            // assistant's 1.5B model in any useful way, so only its 64-bit
            // builds are kept (arm64-v8a for phones, x86_64 for emulators);
            // on a 32-bit device the chat reports that the model could not be
            // loaded instead of crashing (see LlamatikRuntime.native). The
            // rest of the app keeps all four ABIs.
            for (abi in listOf("armeabi-v7a", "x86")) {
                for (library in listOf("ggml", "ggml-base", "ggml-cpu", "llama", "llama-common", "llama_jni", "omp", "whisper")) {
                    excludes += "lib/$abi/lib$library.so"
                }
            }
        }
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
