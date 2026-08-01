package com.studentos.core.intelligence.snapshot

import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.DsaCategoryDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotBuilder @Inject constructor(
    private val subjectDao: SubjectDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val classEventDao: ClassEventDao,
    private val assignmentDao: AssignmentDao,
    private val dsaTopicDao: DsaTopicDao,
    private val dsaCategoryDao: DsaCategoryDao,
    private val projectDao: ProjectDao,
    private val cpProfileDao: CpProfileDao,
    private val settingsDao: SettingsDao,
    private val clock: Clock
) {
    suspend fun build(dateOverrideStr: String? = null): IntelligenceSnapshot = coroutineScope {
        val today = if (dateOverrideStr != null) {
            LocalDate.parse(dateOverrideStr)
        } else {
            LocalDate.now(clock)
        }
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayOfWeekIndex = today.dayOfWeek.value

        val studentContextDeferred = async { getStudentContext() }
        val classesTodayDeferred = async { getClassesToday(dayOfWeekIndex) }
        val attendanceWarningsDeferred = async { getAttendanceWarnings() }
        val assignmentsUrgentDeferred = async { getAssignmentsUrgent() }
        val freeSlotsDeferred = async { getFreeSlots(dayOfWeekIndex) }
        val suggestedDsaTopicDeferred = async { getSuggestedDsaTopic() }
        val suggestedProjectActionDeferred = async { getSuggestedProjectAction() }
        val cpSummaryDeferred = async { getCpSummary() }

        val studentContext = studentContextDeferred.await()
        val classesToday = classesTodayDeferred.await()
        val attendanceWarnings = attendanceWarningsDeferred.await()
        val assignmentsUrgent = assignmentsUrgentDeferred.await()
        val freeSlots = freeSlotsDeferred.await()
        val suggestedDsaTopic = suggestedDsaTopicDeferred.await()
        val suggestedProjectAction = suggestedProjectActionDeferred.await()
        val cpSummary = cpSummaryDeferred.await()

        val scoreTarget = (classesToday.size * 10) +
                (assignmentsUrgent.size * 20) +
                (if (suggestedProjectAction != null) 15 else 0) +
                (if (suggestedDsaTopic != null) 10 else 0)

        IntelligenceSnapshot(
            snapshotVersion = 1,
            date = todayStr,
            studentContext = studentContext,
            classesToday = classesToday,
            attendanceWarnings = attendanceWarnings,
            assignmentsUrgent = assignmentsUrgent,
            freeSlots = freeSlots,
            suggestedDsaTopic = suggestedDsaTopic,
            suggestedProjectAction = suggestedProjectAction,
            score = ScoreSnapshot(target = scoreTarget, actual = 0),
            cpSummary = cpSummary
        )
    }

    private suspend fun getStudentContext(): StudentContextSnapshot {
        val name = settingsDao.get("student_name")
        val tone = settingsDao.get("tone_preference") ?: "motivational"
        return StudentContextSnapshot(name = name, tonePreference = tone)
    }

    private suspend fun getClassesToday(dayOfWeek: Int): List<ClassTodaySnapshot> {
        val allSlots = timetableSlotDao.getAllSlots().first()
        val slotsToday = allSlots.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
        val subjectsMap = subjectDao.getActiveSubjects().first().associateBy { it.id }

        return slotsToday.map { slot ->
            val subjectName = subjectsMap[slot.subjectId]?.name ?: "Subject ${slot.subjectId}"
            ClassTodaySnapshot(
                subject = subjectName,
                time = "${slot.startTime}–${slot.endTime}",
                location = slot.location
            )
        }
    }

    private suspend fun getAttendanceWarnings(): List<AttendanceWarningSnapshot> {
        val summaries = classEventDao.getAllAttendanceSummaries().first()
        val defaultThreshold = settingsDao.get("attendance_threshold")?.toDoubleOrNull() ?: 75.0
        val warnings = mutableListOf<AttendanceWarningSnapshot>()

        for (summary in summaries) {
            val pct = summary.percentage
            if (pct < defaultThreshold) {
                val total = summary.totalHeldCount
                val present = summary.presentCount + summary.extraPresentCount
                val targetFrac = defaultThreshold / 100.0
                var canSkip = 0
                var mustAttend = 0

                if (total > 0) {
                    if (pct >= defaultThreshold) {
                        canSkip = ((present - targetFrac * total) / targetFrac).toInt().coerceAtLeast(0)
                    } else {
                        mustAttend = Math.ceil((targetFrac * total - present) / (1.0 - targetFrac)).toInt().coerceAtLeast(0)
                    }
                }

                warnings.add(
                    AttendanceWarningSnapshot(
                        subject = summary.subjectName,
                        percentage = (Math.round(pct * 10.0) / 10.0),
                        threshold = defaultThreshold,
                        canSkip = canSkip,
                        mustAttend = mustAttend
                    )
                )
            }
        }
        return warnings
    }

    private suspend fun getAssignmentsUrgent(): List<AssignmentUrgentSnapshot> {
        val nowInstant = clock.instant()
        val withinEpoch = nowInstant.plusSeconds(172800).toEpochMilli() // +48 hours
        val urgentEntities = assignmentDao.getUrgentAssignments(withinEpoch)
        val subjectsMap = subjectDao.getActiveSubjects().first().associateBy { it.id }

        return urgentEntities.map { assignment ->
            val hoursRemaining = try {
                val dueInstant = Instant.ofEpochMilli(assignment.deadline)
                Duration.between(nowInstant, dueInstant).toHours()
            } catch (e: Exception) {
                999L
            }

            AssignmentUrgentSnapshot(
                id = assignment.id,
                subject = subjectsMap[assignment.subjectId]?.name ?: "General",
                title = assignment.title,
                deadline = assignment.deadline.toString(),
                status = assignment.status,
                hoursRemaining = hoursRemaining
            )
        }
    }

    private suspend fun getFreeSlots(dayOfWeek: Int): List<FreeSlotSnapshot> {
        val slots = timetableSlotDao.getAllSlots().first()
            .filter { it.dayOfWeek == dayOfWeek }
            .sortedBy { it.startTime }

        if (slots.size < 2) return emptyList()

        val freeSlots = mutableListOf<FreeSlotSnapshot>()
        for (i in 0 until slots.size - 1) {
            val currentEnd = slots[i].endTime
            val nextStart = slots[i + 1].startTime

            try {
                val endLocal = LocalTime.parse(currentEnd)
                val startLocal = LocalTime.parse(nextStart)

                if (startLocal.isAfter(endLocal)) {
                    val duration = Duration.between(endLocal, startLocal).toMinutes().toInt()
                    if (duration >= 30) {
                        freeSlots.add(
                            FreeSlotSnapshot(
                                start = currentEnd,
                                end = nextStart,
                                durationMinutes = duration
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore parse failures
            }
        }
        return freeSlots
    }

    private suspend fun getSuggestedDsaTopic(): SuggestedDsaTopicSnapshot? {
        val topicEntity = dsaTopicDao.getSuggestedTopic() ?: return null
        val categoryEntity = dsaCategoryDao.getCategoryById(topicEntity.categoryId).firstOrNull()

        return SuggestedDsaTopicSnapshot(
            category = categoryEntity?.name ?: "General DSA",
            topic = topicEntity.name,
            confidence = topicEntity.confidenceLevel,
            revisionStatus = topicEntity.revisionStatus
        )
    }

    private suspend fun getSuggestedProjectAction(): SuggestedProjectActionSnapshot? {
        val projectsWithAction = projectDao.getProjectsWithNextAction()
        val firstProjectWithAction = projectsWithAction.firstOrNull { it.nextActionTitle != null }

        return if (firstProjectWithAction != null && firstProjectWithAction.nextActionTitle != null) {
            SuggestedProjectActionSnapshot(
                project = firstProjectWithAction.project.title,
                action = firstProjectWithAction.nextActionTitle!!
            )
        } else {
            null
        }
    }

    private suspend fun getCpSummary(): CpSummarySnapshot {
        val profiles = cpProfileDao.getProfilesForSnapshot().associateBy { it.platform.uppercase() }
        val codechef = profiles["CODECHEF"]
        val codeforces = profiles["CODEFORCES"]

        val lastSynced = codechef?.lastSyncedAt?.toString() ?: codeforces?.lastSyncedAt?.toString()

        return CpSummarySnapshot(
            codechefRating = codechef?.currentRating ?: 0,
            codeforcesRating = codeforces?.currentRating ?: 0,
            lastSynced = lastSynced
        )
    }
}
