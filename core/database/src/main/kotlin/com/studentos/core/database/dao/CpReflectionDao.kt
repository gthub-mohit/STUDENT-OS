package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.CpReflectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * CpReflectionDao — Room DAO interface for `cp_reflections` table operations.
 */
@Dao
interface CpReflectionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reflection: CpReflectionEntity): Long

    @Update
    suspend fun update(reflection: CpReflectionEntity)

    @Query("SELECT * FROM cp_reflections WHERE contest_id = :contestId")
    fun getReflectionForContest(contestId: Long): Flow<CpReflectionEntity?>
}
