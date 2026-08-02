package com.studentos.feature.projects.domain.repository

import com.studentos.feature.projects.domain.model.BugDomain
import com.studentos.feature.projects.domain.model.MilestoneDomain
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectResourceDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getActiveProjects(): Flow<List<ProjectDomain>>
    fun getArchivedProjects(): Flow<List<ProjectDomain>>
    fun getProjectById(id: Long): Flow<ProjectDomain?>
    suspend fun createProject(title: String, inactivityThresholdDays: Int): Long
    suspend fun updateProject(id: Long, title: String, inactivityThresholdDays: Int)
    suspend fun archiveProject(id: Long)
    suspend fun unarchiveProject(id: Long)

    // Task Operations
    fun getTasksForProject(projectId: Long): Flow<List<ProjectTaskDomain>>
    suspend fun createTask(projectId: Long, title: String, isParallel: Boolean): Long
    suspend fun updateTask(taskId: Long, title: String)
    suspend fun deleteTask(taskId: Long)
    suspend fun completeTask(taskId: Long)
    suspend fun reopenTask(taskId: Long)
    suspend fun setNextAction(projectId: Long, taskId: Long)
    suspend fun toggleParallelMode(projectId: Long, isParallel: Boolean)

    // Milestone Operations
    fun getMilestonesForProject(projectId: Long): Flow<List<MilestoneDomain>>
    suspend fun createMilestone(projectId: Long, title: String, description: String?, targetDate: Long?): Long
    suspend fun updateMilestone(id: Long, title: String, description: String?, targetDate: Long?)
    suspend fun deleteMilestone(id: Long)
    suspend fun completeMilestone(id: Long)
    suspend fun reopenMilestone(id: Long)

    // Bug / Issue Tracker Operations
    fun getBugsForProject(projectId: Long): Flow<List<BugDomain>>
    suspend fun createBug(projectId: Long, description: String, severity: String): Long
    suspend fun updateBug(id: Long, description: String, severity: String)
    suspend fun deleteBug(id: Long)
    suspend fun resolveBug(id: Long)
    suspend fun reopenBug(id: Long)

    // Resource Vault & Notes Operations
    fun getResourcesForProject(projectId: Long): Flow<List<ProjectResourceDomain>>
    suspend fun createResource(projectId: Long, url: String, label: String?, type: String): Long
    suspend fun updateResource(id: Long, url: String, label: String?, type: String)
    suspend fun deleteResource(id: Long)
}
