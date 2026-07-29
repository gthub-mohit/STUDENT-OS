/*
 * feature/coding/build.gradle.kts — :feature:coding
 *
 * Stub created in task 0.1. Full implementation in tasks 4.1–4.7.
 *
 * Allowed dependencies: :core:database, :core:events, :core:ui, :core:sync
 * Forbidden: any other :feature:* module.
 * Note: :feature:coding is the ONLY feature module permitted to depend on :core:sync.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.studentos.feature.coding"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
}

dependencies {
    // Hilt — required by the hilt plugin applied above.
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Lint — feature-to-feature dependency guard.
    lintChecks(project(":lint-checks"))

    // Task 4.x: :core:database, :core:events, :core:ui, :core:sync,
    //            Compose, and additional dependencies added here.
}
