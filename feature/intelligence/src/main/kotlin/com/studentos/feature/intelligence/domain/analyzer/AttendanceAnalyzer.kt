package com.studentos.feature.intelligence.domain.analyzer

import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.feature.intelligence.domain.model.fact.AttendanceFact
import com.studentos.feature.intelligence.domain.model.fact.LowAttendanceSubjectFact
import com.studentos.feature.intelligence.domain.model.fact.TimetableSlotFact
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AttendanceAnalyzer @Inject constructor(
    private val subjectDao: SubjectDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val classEventDao: ClassEventDao
) : IntelligenceAnalyzer {

    override val key: String = KEY

    override suspend fun analyze(todayDate: String): AttendanceFact {
        val summaries: List<SubjectAttendanceSummary> = classEventDao.getAllAttendanceSummaries().first()
        val subjects = subjectDao.getActiveSubjects().first().associateBy { it.id }
        val targetPct = 75.0

        val lowAttendance = summaries.mapNotNull { summary ->
            if (summary.totalHeldCount > 0 && summary.percentage < targetPct) {
                LowAttendanceSubjectFact(
                    subjectId = summary.subjectId,
                    subjectName = summary.subjectName,
                    currentPercentage = summary.percentage,
                    targetPercentage = targetPct,
                    isCritical = summary.percentage < (targetPct - 5.0)
                )
            } else {
                null
            }
        }

        val todaySlots = timetableSlotDao.getAllSlots().first().map { slot ->
            val name = subjects[slot.subjectId]?.name ?: "Subject #${slot.subjectId}"
            TimetableSlotFact(
                slotId = slot.id,
                subjectId = slot.subjectId,
                subjectName = name,
                startTime = slot.startTime,
                endTime = slot.endTime
            )
        }

        val mustAttend = lowAttendance.map { it.subjectId }
        val canSkip = summaries.mapNotNull { summary ->
            if (summary.totalHeldCount > 0 && summary.percentage >= (targetPct + 10.0)) {
                summary.subjectId
            } else {
                null
            }
        }

        return AttendanceFact(
            todaySlots = todaySlots,
            lowAttendanceSubjects = lowAttendance,
            mustAttendSubjectIds = mustAttend,
            canSkipSubjectIds = canSkip
        )
    }

    companion object {
        const val KEY = "attendance"
    }
}
