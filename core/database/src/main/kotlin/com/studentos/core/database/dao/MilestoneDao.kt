package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow

/**
 * MilestoneDao — Room DAO interface for `milestones` table operations.
 */
@Dao
interface MilestoneDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(milestone: MilestoneEntity): Long

    @Update
    suspend fun update(milestone: MilestoneEntity)

    @Query("UPDATE milestones SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM milestones WHERE id = :id")
    suspend fun getMilestoneById(id: Long): MilestoneEntity?

    @Query("SELECT * FROM milestones WHERE project_id = :projectId ORDER BY target_date ASC, id ASC")
    fun getMilestonesForProject(projectId: Long): Flow<List<MilestoneEntity>>
}
