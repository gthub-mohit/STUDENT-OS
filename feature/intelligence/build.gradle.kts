/*
 * feature/intelligence/build.gradle.kts — :feature:intelligence
 *
 * Stub created in task 0.1 so settings.gradle.kts resolves cleanly.
 * Full implementation begins in task 6a.1 (IntelligenceOrchestrator).
 *
 * Allowed dependencies (folder-structure.md):
 *   :core:database, :core:events, :core:intelligence, :core:ui, :core:notifications
 * Forbidden: any other :feature:* module.
 * Note: :feature:intelligence is one of only two feature modules permitted
 *       to depend on :core:intelligence (the other is :feature:settings).
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.studentos.feature.intelligence"
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

    // Task 6a.x: :core:database, :core:events, :core:intelligence, :core:ui,
    //             :core:notifications, WorkManager, Compose, and additional dependencies added here.
}
