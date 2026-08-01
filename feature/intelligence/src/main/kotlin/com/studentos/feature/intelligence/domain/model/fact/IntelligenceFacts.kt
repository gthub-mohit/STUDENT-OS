package com.studentos.feature.intelligence.domain.model.fact

data class IntelligenceFacts(
    val date: String,
    val attendance: AttendanceFact = AttendanceFact(),
    val assignments: AssignmentFact = AssignmentFact(),
    val coding: CodingFact = CodingFact(),
    val project: ProjectFact = ProjectFact()
)
