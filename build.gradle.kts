/*
 * build.gradle.kts — Root project build file
 *
 * Applies plugins to the root project without applying them to any module.
 * All submodule build files apply the plugins they need via the version catalog.
 */

// Top-level build file; no source code is compiled here.
// Plugin declarations use the version catalog defined in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application)    apply false
    alias(libs.plugins.android.library)        apply false
    alias(libs.plugins.kotlin.android)         apply false
    alias(libs.plugins.kotlin.serialization)   apply false
    alias(libs.plugins.kotlin.compose)         apply false
    alias(libs.plugins.ksp)                    apply false
    alias(libs.plugins.hilt)                   apply false
}
