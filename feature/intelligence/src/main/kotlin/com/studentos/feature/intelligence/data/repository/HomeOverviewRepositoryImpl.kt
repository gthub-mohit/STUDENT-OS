package com.studentos.feature.intelligence.data.repository

import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.domain.repository.HomeOverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * HomeOverviewRepositoryImpl — Data layer repository that aggregates Home overview data
 * by querying database DAOs and transforming them into domain models.
 *
 * It contains ONLY query/aggregation logic for the Home screen.
 */
class HomeOverviewRepositoryImpl @Inject constructor(
    private val assignmentDao: AssignmentDao,
    private val classEventDao: ClassEventDao,
    private val subjectDao: SubjectDao,
    private val dsaTopicDao: DsaTopicDao,
    private val projectDao: ProjectDao,
    private val clock: Clock
) : HomeOverviewRepository {

    override fun getTodayFocusItems(): Flow<List<TodayFocusItem>> {
        val now = LocalDate.now(clock)
        val zone = clock.zone
        val startOfDayMs = now.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDayMs = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val assignmentsFlow = assignmentDao.getAllAssignments()
        val classEventsFlow = classEventDao.getEventsForDay(startOfDayMs, endOfDayMs)
        val subjectsFlow = subjectDao.getActiveSubjects()
        val dsaTopicsFlow = dsaTopicDao.getAllTopics()
        val projectsFlow = projectDao.getActiveProjects()

        return combine(
            assignmentsFlow,
            classEventsFlow,
            subjectsFlow,
            dsaTopicsFlow,
            projectsFlow
        ) { assignments, classEvents, subjects, dsaTopics, _ ->
            val subjectMap = subjects.associateBy { it.id }
            val items = mutableListOf<TodayFocusItem>()

            // 1. Assignments: Take urgent / due-today / overdue or pending assignments
            val relevantAssignments = assignments
                .filter { it.status == AssignmentEntity.STATUS_PENDING || it.status == AssignmentEntity.STATUS_IN_PROGRESS || it.deadline in startOfDayMs..endOfDayMs || (it.status == AssignmentEntity.STATUS_COMPLETED && it.updatedAt in startOfDayMs..endOfDayMs) }
                .sortedWith(compareBy({ it.status == AssignmentEntity.STATUS_COMPLETED }, { it.deadline }))

            relevantAssignments.take(2).forEach { assignment ->
                val isCompleted = assignment.status == AssignmentEntity.STATUS_COMPLETED || assignment.status == AssignmentEntity.STATUS_SUBMITTED
                val subjectName = subjectMap[assignment.subjectId]?.name
                val subtitle = if (subjectName != null) "$subjectName · Due ${formatFriendlyDate(assignment.deadline, clock)}" else "Due ${formatFriendlyDate(assignment.deadline, clock)}"
                items.add(
                    TodayFocusItem(
                        id = "asgn_${assignment.id}",
                        title = "Finish ${assignment.title}",
                        subtitle = subtitle,
                        category = assignment.taskType,
                        isCompleted = isCompleted,
                        actionRoute = "assignments/list",
                        entityId = assignment.id
                    )
                )
            }

            // 2. Class Events for Today
            classEvents.forEach { event ->
                val isCompleted = event.status == "PRESENT"
                val subjectName = subjectMap[event.subjectId]?.name ?: "Class #${event.subjectId}"
                val timeStr = formatEventTime(event.scheduledAt, clock)
                items.add(
                    TodayFocusItem(
                        id = "class_${event.id}",
                        title = "Attend $subjectName",
                        subtitle = timeStr,
                        category = "ATTENDANCE",
                        isCompleted = isCompleted,
                        actionRoute = "weekly",
                        entityId = event.id
                    )
                )
            }

            // 3. Weak DSA Topic / Topic in Revision
            val dsaFocus = dsaTopics.firstOrNull { it.revisionStatus in listOf("NOT_STARTED", "IN_PROGRESS") || (it.revisionStatus == "REVISED" && it.updatedAt in startOfDayMs..endOfDayMs) }
            if (dsaFocus != null) {
                val isCompleted = dsaFocus.revisionStatus == "REVISED"
                items.add(
                    TodayFocusItem(
                        id = "dsa_${dsaFocus.id}",
                        title = "Practice DSA: ${dsaFocus.name}",
                        subtitle = "Confidence level: ${dsaFocus.confidenceLevel}/5",
                        category = "DSA",
                        isCompleted = isCompleted,
                        actionRoute = "coding/knowledge-tree",
                        entityId = dsaFocus.id
                    )
                )
            }

            // Sort: uncompleted items first, then take maximum 3 actionable priorities
            items.sortedBy { it.isCompleted }.take(3)
        }
    }

    override fun getComingUpItems(): Flow<List<ComingUpItem>> {
        val nowMs = clock.millis()
        val oneWeekLaterMs = nowMs + (14 * 24 * 3600 * 1000L)
        val assignmentsFlow = assignmentDao.getAllAssignments()
        val classEventsFlow = classEventDao.getEventsForWeek(nowMs, oneWeekLaterMs)
        val subjectsFlow = subjectDao.getActiveSubjects()

        return combine(assignmentsFlow, classEventsFlow, subjectsFlow) { assignments, classEvents, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            val upcoming = mutableListOf<ComingUpItem>()

            // 1. Upcoming pending/in-progress assignments and tasks
            assignments
                .filter { (it.status == AssignmentEntity.STATUS_PENDING || it.status == AssignmentEntity.STATUS_IN_PROGRESS) && it.deadline >= (nowMs - 3600_000L) }
                .sortedBy { it.deadline }
                .forEach { assignment ->
                    val subjectName = subjectMap[assignment.subjectId]?.name
                    val dueStr = "Due ${formatFriendlyDate(assignment.deadline, clock)}"
                    val subtitle = if (subjectName != null) "$subjectName · $dueStr" else dueStr
                    upcoming.add(
                        ComingUpItem(
                            id = "asgn_up_${assignment.id}",
                            title = assignment.title,
                            subtitle = subtitle,
                            category = assignment.taskType,
                            actionRoute = "assignments/list",
                            timestamp = assignment.deadline,
                            entityId = assignment.id
                        )
                    )
                }

            // 2. Upcoming classes
            classEvents
                .filter { it.scheduledAt > nowMs && it.status != "CANCELLED" }
                .sortedBy { it.scheduledAt }
                .forEach { event ->
                    val subjectName = subjectMap[event.subjectId]?.name ?: "Class #${event.subjectId}"
                    val timeStr = "${formatFriendlyDate(event.scheduledAt, clock)} · ${formatEventTime(event.scheduledAt, clock)}"
                    upcoming.add(
                        ComingUpItem(
                            id = "class_up_${event.id}",
                            title = subjectName,
                            subtitle = timeStr,
                            category = "CLASS",
                            actionRoute = "weekly",
                            timestamp = event.scheduledAt,
                            entityId = event.id
                        )
                    )
                }

            // Sort all by timestamp ascending
            upcoming.sortedBy { it.timestamp }
        }
    }

    private fun formatFriendlyDate(epochMs: Long, clock: Clock): String {
        val date = Instant.ofEpochMilli(epochMs).atZone(clock.zone).toLocalDate()
        val today = LocalDate.now(clock)
        val tomorrow = today.plusDays(1)
        return when (date) {
            today -> "today"
            tomorrow -> "tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
        }
    }

    private fun formatEventTime(epochMs: Long, clock: Clock): String {
        return try {
            val zdt = Instant.ofEpochMilli(epochMs).atZone(clock.zone)
            zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        } catch (_: Exception) {
            ""
        }
    }
}
