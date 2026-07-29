/*
 * feature/projects/build.gradle.kts — :feature:projects
 *
 * Stub created in task 0.1. Full implementation in tasks 5.1–5.6.
 *
 * Allowed dependencies: :core:database, :core:events, :core:ui, :core:notifications
 * Forbidden: any other :feature:* module.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.studentos.feature.projects"
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

    // Task 5.x: :core:database, :core:events, :core:ui, :core:notifications,
    //            WorkManager, Compose, and additional dependencies added here.
}
