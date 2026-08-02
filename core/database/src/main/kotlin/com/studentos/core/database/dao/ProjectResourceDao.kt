package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.ProjectResourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * ProjectResourceDao — Room DAO interface for `project_resources` table operations.
 */
@Dao
interface ProjectResourceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(resource: ProjectResourceEntity): Long

    @Update
    suspend fun update(resource: ProjectResourceEntity)

    @Query("DELETE FROM project_resources WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM project_resources WHERE id = :id")
    suspend fun getResourceById(id: Long): ProjectResourceEntity?

    @Query("SELECT * FROM project_resources WHERE project_id = :projectId ORDER BY id ASC")
    fun getResourcesForProject(projectId: Long): Flow<List<ProjectResourceEntity>>
}
