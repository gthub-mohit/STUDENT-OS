package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.database.relation.ProjectWithNextAction
import kotlinx.coroutines.flow.Flow

/**
 * ProjectDao — Room DAO interface for `projects` table operations.
 */
@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET last_activity_at = :lastActivityAt WHERE id = :id")
    suspend fun updateLastActivityAt(id: Long, lastActivityAt: Long)

    @Query("UPDATE projects SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)

    @Query("SELECT * FROM projects WHERE archived_at IS NULL ORDER BY last_activity_at DESC")
    fun getActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE archived_at IS NOT NULL ORDER BY archived_at DESC")
    fun getArchivedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE archived_at IS NULL ORDER BY last_activity_at ASC")
    suspend fun getActiveProjectsForInactivityCheck(): List<ProjectEntity>

    @Query("""
        SELECT 
            p.id AS id,
            p.title AS title,
            p.archived_at AS archived_at,
            p.inactivity_threshold_days AS inactivity_threshold_days,
            p.last_activity_at AS last_activity_at,
            t.id AS nextActionId,
            t.title AS nextActionTitle,
            t.is_next_action AS isNextAction,
            t.is_parallel AS isParallel,
            t.completed_at AS completedAt,
            t.sort_order AS sortOrder
        FROM projects p
        LEFT JOIN project_tasks t ON p.id = t.project_id AND t.is_next_action = 1 AND t.completed_at IS NULL
        WHERE p.archived_at IS NULL
        ORDER BY p.last_activity_at DESC
    """)
    suspend fun getProjectsWithNextAction(): List<ProjectWithNextAction>
}
