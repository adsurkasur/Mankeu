import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.mankeu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mankeu"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: System.getenv("KEYSTORE_FILE")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            // R8 for the production release (DEC-023): smaller download. Checked on the emulator with
            // the minified build: create a business, record, reports, stock, help, settings and the
            // sync card. Import, export, backup and a real sync round need the file picker or a Google
            // account and are on the owner's release checklist (docs/audit/2026-10-22_AUDIT_PRA_RILIS.md).
            isMinifyEnabled = true
            isShrinkResources = true
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: System.getenv("KEYSTORE_FILE")
            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // The About card reads BuildConfig.VERSION_NAME instead of a hardcoded literal, so the
        // shown version can no longer drift from the one that was actually built.
        buildConfig = true
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // A Windows PATH containing a quoted segment — for example
    // `"C:\Some\Dir;C:\Other\Dir"`, which is what you get when someone pastes a whole PATH into
    // one entry of the environment editor — breaks argument quoting for the forked Gradle test
    // worker. The worker then dies with
    // `Could not find or load main class <whatever followed the stray quote>`, and the failure
    // looks like a broken build rather than a broken machine setting. Stripping the quotes for
    // the child process costs nothing and keeps `gradlew test` working on a box nobody has
    // cleaned up yet. Directory entries are still all present; only the quote characters go.
    tasks.withType<Test>().configureEach {
        System.getenv("PATH")?.takeIf { it.contains('"') }?.let { path ->
            environment("PATH", path.replace("\"", ""))
        }
    }
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    // Vico: cartesian charts with native horizontal scrolling, pinch zoom and press-to-inspect
    // markers. Replaces two hand-rolled Canvas charts that could not do any of those and clipped
    // their own bars once a book ran past a few months (D-066).
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)

    // Firebase Authentication for user identity (local-first; no analytics per O-011)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    // `org.json` ships with Android, but local JVM tests only see the android.jar stubs of it.
    testImplementation(libs.org.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    // room-testing (MigrationTestHelper) is built against kotlinx-serialization 1.8; the app only
    // pulls 1.7.3 transitively, and instrumented tests share the app's classes, so without this
    // the schema reader dies with AbstractMethodError. A version bump of an existing transitive
    // dependency, not a new one.
    constraints {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    }
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
