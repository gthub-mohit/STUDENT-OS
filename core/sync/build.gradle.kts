/*
 * core/sync/build.gradle.kts — :core:sync
 *
 * Stub created in task 0.1. CP API clients (CodeChef, Codeforces),
 * CpSyncWorker, and ContestReminderWorker are implemented in tasks 4.1–4.2.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.studentos.core.sync"
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

    // Task 4.x: :core:database, :core:events, Retrofit, WorkManager,
    //            and additional dependencies added here.
}
