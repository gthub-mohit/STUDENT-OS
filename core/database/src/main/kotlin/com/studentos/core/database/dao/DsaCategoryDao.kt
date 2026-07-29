package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.DsaCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DsaCategoryDao — Room DAO interface for `dsa_categories` table operations.
 */
@Dao
interface DsaCategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: DsaCategoryEntity): Long

    @Update
    suspend fun update(category: DsaCategoryEntity)

    @Query("DELETE FROM dsa_categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM dsa_categories ORDER BY sort_order ASC, name ASC")
    fun getAllCategories(): Flow<List<DsaCategoryEntity>>

    @Query("SELECT * FROM dsa_categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<DsaCategoryEntity?>

    @Query("SELECT COUNT(*) FROM dsa_categories")
    suspend fun getCategoryCount(): Int
}
