package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.BugEntity
import kotlinx.coroutines.flow.Flow

/**
 * BugDao — Room DAO interface for `bugs` table operations.
 */
@Dao
interface BugDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bug: BugEntity): Long

    @Update
    suspend fun update(bug: BugEntity)

    @Query("UPDATE bugs SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM bugs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM bugs WHERE project_id = :projectId ORDER BY id ASC")
    fun getBugsForProject(projectId: Long): Flow<List<BugEntity>>

    @Query("SELECT * FROM bugs WHERE project_id = :projectId AND status = 'OPEN' ORDER BY id ASC")
    fun getOpenBugsForProject(projectId: Long): Flow<List<BugEntity>>

    @Query("SELECT COUNT(*) FROM bugs WHERE project_id = :projectId AND status = 'OPEN'")
    suspend fun getOpenBugCount(projectId: Long): Int
}
