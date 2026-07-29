package com.studentos.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/**
 * Registry of custom lint issues for Student OS.
 *
 * Currently registers only [FeatureToFeatureDependencyDetector.ISSUE].
 */
class StudentOsIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        FeatureToFeatureDependencyDetector.ISSUE
    )

    override val api: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "Student OS",
        identifier = "com.studentos.lint",
        feedbackUrl = "https://github.com/studentos/studentos/issues"
    )
}
