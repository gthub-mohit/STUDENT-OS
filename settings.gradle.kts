/*
 * settings.gradle.kts — Student OS
 *
 * Declares the root project name and all Gradle sub-modules.
 * Module structure follows folder-structure.md exactly.
 * Modules for tasks 0.2+ are declared here now so the project
 * can resolve them; their build.gradle.kts files are created in task 0.2.
 */

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StudentOS"

// Shell module
include(":app")

// Custom lint checks (feature-to-feature dependency guard)
include(":lint-checks")

// Core modules
include(":core:database")
include(":core:ui")
include(":core:events")
include(":core:intelligence")
include(":core:notifications")
include(":core:sync")

// Feature modules
include(":feature:attendance")
include(":feature:assignments")
include(":feature:coding")
include(":feature:projects")
include(":feature:intelligence")
include(":feature:settings")
