package com.studentos.feature.intelligence.domain.analyzer

import com.studentos.feature.intelligence.domain.model.fact.ProjectFact
import javax.inject.Inject

class ProjectAnalyzerStub @Inject constructor() : IntelligenceAnalyzer {

    override val key: String = KEY

    override suspend fun analyze(todayDate: String): ProjectFact {
        return ProjectFact(
            activeProjectCount = 0,
            pendingNextActionsCount = 0
        )
    }

    companion object {
        const val KEY = "project"
    }
}
