/*
 * core/notifications/build.gradle.kts — :core:notifications
 *
 * Stub created in task 0.1. NotificationChannelRegistry, NotificationScheduler,
 * and NotificationRescheduler are implemented in task 7.1 and 7.4.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.studentos.core.notifications"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Hilt — required by the hilt plugin applied above.
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Task 7.x: :core:database, WorkManager, and additional dependencies added here.
}
