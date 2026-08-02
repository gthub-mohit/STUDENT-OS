/*
 * feature/projects/build.gradle.kts — :feature:projects
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
    // Core module dependencies
    implementation(project(":core:database"))
    implementation(project(":core:events"))

    // Compose BOM & UI
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.composeUi)

    // Navigation & Hilt
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Lint — feature-to-feature dependency guard.
    lintChecks(project(":lint-checks"))

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
}
