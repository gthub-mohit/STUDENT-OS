/*
 * core/ui/build.gradle.kts — :core:ui
 *
 * Stub created in task 0.1. Full theme, typography, and shared components
 * are implemented in task 0.2.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace  = "com.studentos.core.ui"
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
    // Task 0.2: Compose BOM, Material 3, and shared component dependencies added here.
}
