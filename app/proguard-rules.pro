# proguard-rules.pro — :app module
#
# Release ProGuard rules for Student OS.
# Rules for specific libraries (Room, Retrofit, ML Kit, Jetpack Security)
# are added in task 10.6 when release build preparation is done.
# This file is intentionally minimal for the scaffolding phase.

# Keep the application class (required by Hilt).
-keep class com.studentos.app.StudentOsApp { *; }

# Keep all Hilt-generated classes.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin metadata used by reflection.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
