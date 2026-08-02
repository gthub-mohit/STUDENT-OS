/*
 * app/build.gradle.kts — :app module
 *
 * The shell module. Owns navigation, DI wiring, and the single Activity.
 * All feature and core modules are declared as dependencies here so the
 * final APK includes every module.
 *
 * Tasks implemented here: 0.1 (SDK config, Compose BOM, basic dependencies).
 * Tasks 0.2–0.4 will add module dependencies and Navigation wiring.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.studentos.app"

    // ── SDK versions (task 0.1 requirement) ──────────────────────────────────
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.studentos.app"
        minSdk        = libs.versions.minSdk.get().toInt()     // 26 — Android 8.0
        targetSdk     = libs.versions.targetSdk.get().toInt()  // 35
        versionCode   = 1
        versionName   = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
            isMinifyEnabled     = false
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ── KSP configuration ────────────────────────────────────────────────────────
// Room schema export path for migration validation (task 1.x).
// Must be a top-level block — NOT nested inside android {} — per KSP + AGP 8.x.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // ── Compose BOM ──────────────────────────────────────────────────────────
    // Importing the BOM aligns all compose-* library versions automatically.
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ── Core Android ─────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.activity.compose)
    // AppCompat required for Theme.AppCompat.DayNight.NoActionBar in themes.xml
    implementation(libs.androidx.appcompat)

    // ── Compose UI ───────────────────────────────────────────────────────────
    // compose-ui bundle in libs.versions.toml — hyphen becomes camelCase in Kotlin DSL.
    implementation(libs.bundles.composeUi)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── Navigation ───────────────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Hilt ─────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── WorkManager ──────────────────────────────────────────────────────────
    // Required because StudentOsApp implements Configuration.Provider
    // (imports androidx.work.Configuration). HiltWorkerFactory wired in task 0.3.
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // ── Testing ──────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)

    // ── Feature and Core modules ─────────────────────────────────────────────
    implementation(project(":core:database"))
    // implementation(project(":core:ui"))
    // implementation(project(":core:events"))
    // implementation(project(":core:intelligence"))
    // implementation(project(":core:notifications"))
    // implementation(project(":core:sync"))
    implementation(project(":feature:attendance"))
    implementation(project(":feature:assignments"))
    implementation(project(":feature:coding"))
    implementation(project(":feature:intelligence"))
    implementation(project(":feature:projects"))
    implementation(project(":feature:settings"))
}
