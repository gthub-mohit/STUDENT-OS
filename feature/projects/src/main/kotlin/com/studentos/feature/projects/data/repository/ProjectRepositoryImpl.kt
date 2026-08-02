package com.studentos.feature.projects.data.repository

import com.studentos.core.database.dao.BugDao
import com.studentos.core.database.dao.MilestoneDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.ProjectResourceDao
import com.studentos.core.database.dao.ProjectTaskDao
import com.studentos.core.database.entity.BugEntity
import com.studentos.core.database.entity.MilestoneEntity
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.database.entity.ProjectResourceEntity
import com.studentos.core.database.entity.ProjectTaskEntity
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.projects.domain.model.BugDomain
import com.studentos.feature.projects.domain.model.MilestoneDomain
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectResourceDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectTaskDao: ProjectTaskDao,
    private val milestoneDao: MilestoneDao,
    private val bugDao: BugDao,
    private val projectResourceDao: ProjectResourceDao,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) : ProjectRepository {

    override fun getActiveProjects(): Flow<List<ProjectDomain>> {
        return projectDao.getActiveProjects().map { entities ->
            entities.map { entity ->
                mapToDomain(entity)
            }
        }
    }

    override fun getArchivedProjects(): Flow<List<ProjectDomain>> {
        return projectDao.getArchivedProjects().map { entities ->
            entities.map { entity ->
                mapToDomain(entity)
            }
        }
    }

    override fun getProjectById(id: Long): Flow<ProjectDomain?> {
        return projectDao.getProjectById(id).map { entity ->
            entity?.let { mapToDomain(it) }
        }
    }

    private suspend fun mapToDomain(entity: ProjectEntity): ProjectDomain {
        val tasks = try {
            projectTaskDao.getTasksForProject(entity.id).firstOrNull() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val nextAction = try {
            projectTaskDao.getNextAction(entity.id).firstOrNull()
        } catch (_: Exception) {
            null
        }

        val total = tasks.size
        val completed = tasks.count { it.completedAt != null }

        return ProjectDomain(
            id = entity.id,
            title = entity.title,
            archivedAt = entity.archivedAt,
            inactivityThresholdDays = entity.inactivityThresholdDays,
            lastActivityAt = entity.lastActivityAt,
            nextActionId = nextAction?.id,
            nextActionTitle = nextAction?.title,
            totalTasks = total,
            completedTasks = completed
        )
    }

    override suspend fun createProject(title: String, inactivityThresholdDays: Int): Long {
        val now = clock.millis()
        val entity = ProjectEntity(
            title = title.trim(),
            inactivityThresholdDays = inactivityThresholdDays.coerceAtLeast(1),
            lastActivityAt = now,
            archivedAt = null
        )
        return projectDao.insert(entity)
    }

    override suspend fun updateProject(id: Long, title: String, inactivityThresholdDays: Int) {
        val now = clock.millis()
        val entity = ProjectEntity(
            id = id,
            title = title.trim(),
            inactivityThresholdDays = inactivityThresholdDays.coerceAtLeast(1),
            lastActivityAt = now
        )
        projectDao.update(entity)
        appEventBus.emit(AppEvent.ProjectUpdated(id))
    }

    override suspend fun archiveProject(id: Long) {
        val now = clock.millis()
        projectDao.archive(id, now)
        appEventBus.emit(AppEvent.ProjectUpdated(id))
    }

    override suspend fun unarchiveProject(id: Long) {
        projectDao.archive(id, null)
        val now = clock.millis()
        projectDao.updateLastActivityAt(id, now)
        appEventBus.emit(AppEvent.ProjectUpdated(id))
    }

    // ── Task Operations ───────────────────────────────────────────────────────

    override fun getTasksForProject(projectId: Long): Flow<List<ProjectTaskDomain>> {
        return projectTaskDao.getTasksForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createTask(projectId: Long, title: String, isParallel: Boolean): Long {
        val maxSort = projectTaskDao.getMaxSortOrder(projectId) ?: -1
        val existingNext = projectTaskDao.getNextAction(projectId).firstOrNull()
        val shouldBeNext = existingNext == null

        val entity = ProjectTaskEntity(
            projectId = projectId,
            title = title.trim(),
            isNextAction = shouldBeNext,
            isParallel = isParallel,
            completedAt = null,
            sortOrder = maxSort + 1
        )
        val taskId = projectTaskDao.insert(entity)
        val now = clock.millis()
        projectDao.updateLastActivityAt(projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(projectId))
        return taskId
    }

    override suspend fun updateTask(taskId: Long, title: String) {
        val existing = projectTaskDao.getTaskById(taskId) ?: return
        val updated = existing.copy(title = title.trim())
        projectTaskDao.update(updated)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun deleteTask(taskId: Long) {
        val existing = projectTaskDao.getTaskById(taskId) ?: return
        projectTaskDao.deleteById(taskId)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)

        if (existing.isNextAction) {
            val nextUnfinished = projectTaskDao.getFirstUnfinishedTask(existing.projectId)
            if (nextUnfinished != null) {
                projectTaskDao.updateNextAction(nextUnfinished.id, true)
            }
        }
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun completeTask(taskId: Long) {
        val existing = projectTaskDao.getTaskById(taskId) ?: return
        val now = clock.millis()

        projectTaskDao.updateCompletedAt(taskId, now)
        projectDao.updateLastActivityAt(existing.projectId, now)

        if (existing.isNextAction) {
            val nextUnfinished = projectTaskDao.getFirstUnfinishedTask(existing.projectId)
            if (nextUnfinished != null) {
                projectTaskDao.updateNextAction(nextUnfinished.id, true)
            }
        }

        appEventBus.emit(AppEvent.ProjectTaskCompleted(taskId, existing.projectId))
    }

    override suspend fun reopenTask(taskId: Long) {
        val existing = projectTaskDao.getTaskById(taskId) ?: return
        val now = clock.millis()

        projectTaskDao.updateCompletedAt(taskId, null)
        projectDao.updateLastActivityAt(existing.projectId, now)

        val currentNext = projectTaskDao.getNextAction(existing.projectId).firstOrNull()
        if (currentNext == null) {
            projectTaskDao.updateNextAction(taskId, true)
        }

        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun setNextAction(projectId: Long, taskId: Long) {
        projectTaskDao.clearNextActionForProject(projectId)
        projectTaskDao.updateNextAction(taskId, true)
        val now = clock.millis()
        projectDao.updateLastActivityAt(projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(projectId))
    }

    override suspend fun toggleParallelMode(projectId: Long, isParallel: Boolean) {
        projectTaskDao.updateParallelMode(projectId, isParallel)
        val now = clock.millis()
        projectDao.updateLastActivityAt(projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(projectId))
    }

    // ── Milestone Operations ──────────────────────────────────────────────────

    override fun getMilestonesForProject(projectId: Long): Flow<List<MilestoneDomain>> {
        return milestoneDao.getMilestonesForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createMilestone(
        projectId: Long,
        title: String,
        description: String?,
        targetDate: Long?
    ): Long {
        val entity = MilestoneEntity(
            projectId = projectId,
            title = title.trim(),
            description = description?.trim(),
            targetDate = targetDate,
            status = MilestoneEntity.STATUS_PENDING
        )
        val id = milestoneDao.insert(entity)
        val now = clock.millis()
        projectDao.updateLastActivityAt(projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(projectId))
        return id
    }

    override suspend fun updateMilestone(
        id: Long,
        title: String,
        description: String?,
        targetDate: Long?
    ) {
        val existing = milestoneDao.getMilestoneById(id) ?: return
        val updated = existing.copy(
            title = title.trim(),
            description = description?.trim(),
            targetDate = targetDate
        )
        milestoneDao.update(updated)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun deleteMilestone(id: Long) {
        val existing = milestoneDao.getMilestoneById(id) ?: return
        milestoneDao.deleteById(id)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun completeMilestone(id: Long) {
        val existing = milestoneDao.getMilestoneById(id) ?: return
        milestoneDao.updateStatus(id, MilestoneEntity.STATUS_DONE)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun reopenMilestone(id: Long) {
        val existing = milestoneDao.getMilestoneById(id) ?: return
        milestoneDao.updateStatus(id, MilestoneEntity.STATUS_PENDING)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    // ── Bug / Issue Tracker Operations ────────────────────────────────────────

    override fun getBugsForProject(projectId: Long): Flow<List<BugDomain>> {
        return bugDao.getBugsForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createBug(projectId: Long, description: String, severity: String): Long {
        val entity = BugEntity(
            projectId = projectId,
            description = description.trim(),
            severity = severity.uppercase(),
            status = BugEntity.STATUS_OPEN
        )
        val id = bugDao.insert(entity)
        val now = clock.millis()
        projectDao.updateLastActivityAt(projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(projectId))
        return id
    }

    override suspend fun updateBug(id: Long, description: String, severity: String) {
        val existing = bugDao.getBugById(id) ?: return
        val updated = existing.copy(
            description = description.trim(),
            severity = severity.uppercase()
        )
        bugDao.update(updated)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun deleteBug(id: Long) {
        val existing = bugDao.getBugById(id) ?: return
        bugDao.deleteById(id)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun resolveBug(id: Long) {
        val existing = bugDao.getBugById(id) ?: return
        bugDao.updateStatus(id, BugEntity.STATUS_RESOLVED)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun reopenBug(id: Long) {
        val existing = bugDao.getBugById(id) ?: return
        bugDao.updateStatus(id, BugEntity.STATUS_OPEN)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    // ── Resource Vault Operations ─────────────────────────────────────────────

    override fun getResourcesForProject(projectId: Long): Flow<List<ProjectResourceDomain>> {
        return projectResourceDao.getResourcesForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createResource(
        projectId: Long,
        url: String,
        label: String?,
        type: String
    ): Long {
        val entity = ProjectResourceEntity(
            projectId = projectId,
            url = url.trim(),
            label = label?.trim()
        )
        val id = projectResourceDao.insert(entity)
        val now = clock.millis()
        projectDao.updateLastActivityAt(projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(projectId))
        return id
    }

    override suspend fun updateResource(
        id: Long,
        url: String,
        label: String?,
        type: String
    ) {
        val existing = projectResourceDao.getResourceById(id) ?: return
        val updated = existing.copy(
            url = url.trim(),
            label = label?.trim()
        )
        projectResourceDao.update(updated)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    override suspend fun deleteResource(id: Long) {
        val existing = projectResourceDao.getResourceById(id) ?: return
        projectResourceDao.deleteById(id)
        val now = clock.millis()
        projectDao.updateLastActivityAt(existing.projectId, now)
        appEventBus.emit(AppEvent.ProjectUpdated(existing.projectId))
    }

    private fun ProjectTaskEntity.toDomain() = ProjectTaskDomain(
        id = id,
        projectId = projectId,
        title = title,
        isNextAction = isNextAction,
        isParallel = isParallel,
        completedAt = completedAt,
        sortOrder = sortOrder
    )

    private fun MilestoneEntity.toDomain() = MilestoneDomain(
        id = id,
        projectId = projectId,
        title = title,
        description = description,
        targetDate = targetDate,
        status = status
    )

    private fun BugEntity.toDomain() = BugDomain(
        id = id,
        projectId = projectId,
        description = description,
        severity = severity,
        status = status
    )

    private fun ProjectResourceEntity.toDomain() = ProjectResourceDomain(
        id = id,
        projectId = projectId,
        url = url,
        label = label
    )
}
