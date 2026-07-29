/*
 * lint-checks/build.gradle.kts — :lint-checks
 *
 * Pure JVM module that defines custom Android Lint detectors.
 * Currently contains only the FeatureToFeatureDependencyDetector
 * which prevents :feature:* modules from depending on each other.
 *
 * This module is consumed via `lintChecks(project(":lint-checks"))`
 * in each feature module's build.gradle.kts.
 */
plugins {
    kotlin("jvm")
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)

    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}
