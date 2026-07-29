/*
 * feature/settings/build.gradle.kts — :feature:settings
 *
 * Stub created in task 0.1 so settings.gradle.kts resolves cleanly.
 * Full implementation begins in task 9.1 (SettingsScreen).
 *
 * Allowed dependencies (folder-structure.md):
 *   :core:database, :core:intelligence, :core:ui
 * Forbidden: any other :feature:* module.
 * Note: :feature:settings is the only feature module that provides BackupRepository,
 *       which depends on all DAOs through :core:database. This is an intentional
 *       exception documented in folder-structure.md — it does NOT violate module
 *       boundaries because all DAOs are accessed via :core:database, not via
 *       other feature modules.
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
    namespace  = "com.studentos.feature.settings"
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
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.composeUi)

    // Hilt — required by the hilt plugin applied above.
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Core module dependencies
    implementation(project(":core:database"))

    // Lint — feature-to-feature dependency guard.
    lintChecks(project(":lint-checks"))
}
