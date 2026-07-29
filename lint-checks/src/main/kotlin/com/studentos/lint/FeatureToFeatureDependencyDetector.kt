package com.studentos.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.GradleContext
import com.android.tools.lint.detector.api.GradleScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

/**
 * Lint detector that prevents `:feature:*` modules from depending on
 * other `:feature:*` modules.
 *
 * This enforces the architectural rule from folder-structure.md §1:
 * "FORBIDDEN for every :feature:* module: importing any other :feature:* module."
 *
 * The detector scans Gradle build files for `project(":feature:...")` calls.
 * When found inside a module whose path contains `/feature/`, it reports
 * an error.
 */
class FeatureToFeatureDependencyDetector : Detector(), GradleScanner {

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "FeatureToFeatureDependency",
            briefDescription = "Feature modules must not depend on other feature modules",
            explanation = """
                The Student OS architecture forbids any `:feature:*` module from \
                depending on another `:feature:*` module. All cross-feature \
                communication must go through `:core:*` modules (`:core:database`, \
                `:core:events`, etc.). This keeps feature modules independent and \
                prevents circular dependencies.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                FeatureToFeatureDependencyDetector::class.java,
                Scope.GRADLE_SCOPE
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> = listOf("project")

    override fun checkMethodCall(
        context: GradleContext,
        statement: String,
        parent: String?,
        parentParent: String?,
        namedArguments: Map<String, String>,
        unnamedArguments: List<String>,
        cookie: Any
    ) {
        // Only check modules under the feature/ directory.
        val projectPath = context.project.dir.absolutePath.replace('\\', '/')
        if (!projectPath.contains("/feature/")) return

        // Check if the project() argument references another feature module.
        for (arg in unnamedArguments) {
            val cleaned = arg.trim().removeSurrounding("\"").removeSurrounding("'")
            if (cleaned.startsWith(":feature:")) {
                context.report(
                    ISSUE,
                    cookie,
                    context.getLocation(cookie),
                    "Feature module must not depend on another feature module: `$cleaned`. " +
                        "Use a `:core:*` module for shared logic."
                )
            }
        }
    }
}
