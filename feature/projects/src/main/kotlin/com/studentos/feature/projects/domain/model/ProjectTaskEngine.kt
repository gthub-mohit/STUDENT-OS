package com.studentos.feature.projects.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class NextActionRecommendation(
    val task: ProjectTaskDomain? = null,
    val status: NextActionStatus = NextActionStatus.NONE,
    val title: String = "",
    val reason: String = "",
    val deadlineContext: String? = null,
    val blockerTask: ProjectTaskDomain? = null
)

enum class NextActionStatus {
    AVAILABLE_TASK,
    WAITING_ON_DEPENDENCIES,
    PROJECT_COMPLETE,
    NONE
}

object ProjectTaskEngine {

    /**
     * Resolves the task state:
     * - COMPLETED: task has completedAt != null
     * - AVAILABLE: task is not completed, and has no dependency OR its dependency is completed
     * - BLOCKED: task is not completed, and its dependency is not completed
     */
    fun getTaskState(
        task: ProjectTaskDomain,
        allTasksMap: Map<Long, ProjectTaskDomain>
    ): ProjectTaskState {
        if (task.isCompleted) return ProjectTaskState.COMPLETED
        val depId = task.dependencyTaskId ?: return ProjectTaskState.AVAILABLE
        val depTask = allTasksMap[depId]
        return if (depTask != null && depTask.isCompleted) {
            ProjectTaskState.AVAILABLE
        } else {
            ProjectTaskState.BLOCKED
        }
    }

    /**
     * Returns the incomplete blocker task for a blocked task, if any.
     */
    fun getBlockerTask(
        task: ProjectTaskDomain,
        allTasksMap: Map<Long, ProjectTaskDomain>
    ): ProjectTaskDomain? {
        val depId = task.dependencyTaskId ?: return null
        val depTask = allTasksMap[depId]
        return if (depTask != null && !depTask.isCompleted) depTask else null
    }

    /**
     * Detects self-dependency or cycle in the dependency graph.
     */
    fun wouldCreateCycle(
        taskId: Long,
        targetDependencyId: Long?,
        existingTasks: List<ProjectTaskDomain>
    ): Boolean {
        if (targetDependencyId == null) return false
        if (taskId != 0L && targetDependencyId == taskId) return true

        val taskMap = existingTasks.associateBy { it.id }
        var currentId: Long? = targetDependencyId
        val visited = mutableSetOf<Long>()

        while (currentId != null) {
            if (taskId != 0L && currentId == taskId) return true
            if (!visited.add(currentId)) return true
            currentId = taskMap[currentId]?.dependencyTaskId
        }
        return false
    }

    /**
     * Computes the single deterministic Next Action recommendation.
     * Priority:
     * 1. Available + Overdue
     * 2. Available + Due today
     * 3. Available + Highest priority (HIGH > MEDIUM > LOW)
     * 4. Available + Nearest deadline
     * 5. Available + normal task ordering (sortOrder ASC, id ASC)
     */
    fun computeNextAction(
        tasks: List<ProjectTaskDomain>,
        nowMs: Long = System.currentTimeMillis()
    ): NextActionRecommendation {
        if (tasks.isEmpty()) {
            return NextActionRecommendation(status = NextActionStatus.NONE)
        }

        val unfinished = tasks.filter { !it.isCompleted }
        if (unfinished.isEmpty()) {
            return NextActionRecommendation(
                status = NextActionStatus.PROJECT_COMPLETE,
                title = "Project complete",
                reason = "All ${tasks.size} tasks completed."
            )
        }

        val taskMap = tasks.associateBy { it.id }
        val availableTasks = unfinished.filter { getTaskState(it, taskMap) == ProjectTaskState.AVAILABLE }

        if (availableTasks.isEmpty()) {
            // Everything unfinished is blocked
            val firstBlocked = unfinished.firstOrNull { getTaskState(it, taskMap) == ProjectTaskState.BLOCKED }
            val blocker = firstBlocked?.let { getBlockerTask(it, taskMap) }
            val blockerTitle = blocker?.title ?: "prerequisite task"
            return NextActionRecommendation(
                status = NextActionStatus.WAITING_ON_DEPENDENCIES,
                title = "Waiting on dependencies",
                reason = "Complete '$blockerTitle' to unlock the next task.",
                blockerTask = blocker
            )
        }

        val comparator = Comparator<ProjectTaskDomain> { a, b ->
            val aOverdue = a.deadline != null && a.deadline < nowMs && !isSameDay(a.deadline, nowMs)
            val bOverdue = b.deadline != null && b.deadline < nowMs && !isSameDay(b.deadline, nowMs)
            if (aOverdue != bOverdue) return@Comparator if (aOverdue) -1 else 1

            val aDueToday = a.deadline != null && isSameDay(a.deadline, nowMs)
            val bDueToday = b.deadline != null && isSameDay(b.deadline, nowMs)
            if (aDueToday != bDueToday) return@Comparator if (aDueToday) -1 else 1

            val aPriorityScore = when (a.priority) {
                ProjectTaskPriority.HIGH -> 3
                ProjectTaskPriority.MEDIUM -> 2
                ProjectTaskPriority.LOW -> 1
            }
            val bPriorityScore = when (b.priority) {
                ProjectTaskPriority.HIGH -> 3
                ProjectTaskPriority.MEDIUM -> 2
                ProjectTaskPriority.LOW -> 1
            }
            if (aPriorityScore != bPriorityScore) return@Comparator bPriorityScore.compareTo(aPriorityScore)

            val aDeadline = a.deadline ?: Long.MAX_VALUE
            val bDeadline = b.deadline ?: Long.MAX_VALUE
            if (aDeadline != bDeadline) return@Comparator aDeadline.compareTo(bDeadline)

            if (a.sortOrder != b.sortOrder) return@Comparator a.sortOrder.compareTo(b.sortOrder)
            a.id.compareTo(b.id)
        }

        val chosen = availableTasks.minWith(comparator)
        val reason = when {
            chosen.deadline != null && chosen.deadline < nowMs && !isSameDay(chosen.deadline, nowMs) -> "Available · Overdue"
            chosen.deadline != null && isSameDay(chosen.deadline, nowMs) -> "Available · Due today"
            chosen.priority == ProjectTaskPriority.HIGH -> "Available · High priority"
            chosen.dependencyTaskId != null && taskMap[chosen.dependencyTaskId]?.isCompleted == true -> {
                val depTitle = taskMap[chosen.dependencyTaskId]?.title
                if (!depTitle.isNullOrBlank()) "$depTitle is complete" else "Prerequisite is complete"
            }
            else -> "Available to start"
        }

        return NextActionRecommendation(
            task = chosen,
            status = NextActionStatus.AVAILABLE_TASK,
            title = chosen.title,
            reason = reason,
            deadlineContext = formatDeadlineContext(chosen.deadline, nowMs)
        )
    }

    fun formatDeadlineContext(deadline: Long?, nowMs: Long = System.currentTimeMillis()): String? {
        if (deadline == null) return null
        val zone = ZoneId.systemDefault()
        val deadlineDate = Instant.ofEpochMilli(deadline).atZone(zone).toLocalDate()
        val nowDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

        val daysBetween = ChronoUnit.DAYS.between(nowDate, deadlineDate)
        return when {
            daysBetween < 0 -> "Overdue"
            daysBetween == 0L -> "Due today"
            daysBetween == 1L -> "Due tomorrow"
            daysBetween in 2..7 -> "Due in $daysBetween days"
            else -> "Due ${deadlineDate.dayOfMonth} ${deadlineDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
        }
    }

    private fun isSameDay(timeMsA: Long, timeMsB: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val dateA = Instant.ofEpochMilli(timeMsA).atZone(zone).toLocalDate()
        val dateB = Instant.ofEpochMilli(timeMsB).atZone(zone).toLocalDate()
        return dateA == dateB
    }
}
