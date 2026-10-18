package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * GoalDao — Room DAO interface for `goals` table operations.
 */
@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>): List<Long>

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeGoalById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE is_archived = 0 ORDER BY target_date ASC")
    fun observeActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE is_archived = 1 ORDER BY target_date ASC")
    fun observeArchivedGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY target_date ASC")
    fun observeAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE is_archived = 0 ORDER BY target_date ASC")
    suspend fun getActiveGoals(): List<GoalEntity>

    @Query("UPDATE goals SET is_archived = :isArchived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, updatedAt: Long)
}
