package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studentos.core.database.entity.CpProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * CpProfileDao — Room DAO interface for `cp_profiles` table operations.
 */
@Dao
interface CpProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CpProfileEntity): Long

    @Query("UPDATE cp_profiles SET current_rating = :rating, last_synced_at = :syncTime WHERE id = :id")
    suspend fun updateRatingAndSyncTime(id: Long, rating: Int?, syncTime: Long)

    @Query("SELECT * FROM cp_profiles WHERE platform = :platform")
    fun getProfileByPlatform(platform: String): Flow<CpProfileEntity?>

    @Query("SELECT * FROM cp_profiles ORDER BY platform ASC")
    fun getAllProfiles(): Flow<List<CpProfileEntity>>

    @Query("SELECT * FROM cp_profiles ORDER BY platform ASC")
    suspend fun getProfilesForSnapshot(): List<CpProfileEntity>
}
