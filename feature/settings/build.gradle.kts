/*
 * feature/settings/build.gradle.kts — :feature:settings
 *
 * Full implementation for task 9.1 (SettingsScreen), 9.1a (AI Settings & Diagnostics),
 * Unit 7.5 (Notification Settings), and Unit 8 (Backup & Restore).
 *
 * Allowed dependencies (folder-structure.md):
 *   :core:database, :core:intelligence, :core:ui
 * Forbidden: any other :feature:* module.
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
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Core module dependencies
    implementation(project(":core:database"))
    implementation(project(":core:intelligence"))
    implementation(project(":core:notifications"))
    implementation(project(":core:sync"))
    implementation(project(":core:ui"))

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    // Lint — feature-to-feature dependency guard.
    lintChecks(project(":lint-checks"))
}
