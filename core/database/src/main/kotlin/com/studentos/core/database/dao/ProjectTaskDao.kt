package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.ProjectTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * ProjectTaskDao — Room DAO interface for `project_tasks` table operations.
 */
@Dao
interface ProjectTaskDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: ProjectTaskEntity): Long

    @Update
    suspend fun update(task: ProjectTaskEntity)

    @Query("UPDATE project_tasks SET is_next_action = :isNextAction WHERE id = :id")
    suspend fun updateNextAction(id: Long, isNextAction: Boolean)

    @Query("UPDATE project_tasks SET completed_at = :completedAt WHERE id = :id")
    suspend fun updateCompletedAt(id: Long, completedAt: Long?)

    @Query("UPDATE project_tasks SET is_parallel = :isParallel WHERE project_id = :projectId")
    suspend fun updateParallelMode(projectId: Long, isParallel: Boolean)

    @Query("DELETE FROM project_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM project_tasks WHERE project_id = :projectId AND completed_at IS NOT NULL")
    suspend fun deleteCompletedTasksForProject(projectId: Long)

    @Query("SELECT * FROM project_tasks WHERE project_id = :projectId ORDER BY sort_order ASC, id ASC")
    fun getTasksForProject(projectId: Long): Flow<List<ProjectTaskEntity>>

    @Query("SELECT * FROM project_tasks WHERE project_id = :projectId AND is_next_action = 1 AND completed_at IS NULL LIMIT 1")
    fun getNextAction(projectId: Long): Flow<ProjectTaskEntity?>

    @Query("SELECT COUNT(*) FROM project_tasks WHERE project_id = :projectId AND completed_at IS NULL")
    suspend fun getPendingTaskCount(projectId: Long): Int
}
