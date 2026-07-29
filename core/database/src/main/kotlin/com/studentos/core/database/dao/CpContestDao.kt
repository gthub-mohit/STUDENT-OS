package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studentos.core.database.entity.CpContestEntity
import kotlinx.coroutines.flow.Flow

/**
 * CpContestDao — Room DAO interface for `cp_contests` table operations.
 */
@Dao
interface CpContestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContests(contests: List<CpContestEntity>): List<Long>

    @Query("SELECT * FROM cp_contests WHERE profile_id = :profileId ORDER BY contest_date DESC")
    fun getContestsByProfile(profileId: Long): Flow<List<CpContestEntity>>

    @Query("SELECT * FROM cp_contests WHERE profile_id = :profileId ORDER BY contest_date DESC LIMIT :limit")
    fun getRecentContests(profileId: Long, limit: Int): Flow<List<CpContestEntity>>

    @Query("SELECT * FROM cp_contests WHERE contest_date >= :nowEpoch AND contest_date <= :lookaheadEpoch ORDER BY contest_date ASC")
    suspend fun getUpcomingContests(nowEpoch: Long, lookaheadEpoch: Long): List<CpContestEntity>
}
