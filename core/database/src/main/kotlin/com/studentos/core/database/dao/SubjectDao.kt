package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * SubjectDao — Room DAO interface for `subjects` table operations.
 */
@Dao
interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(subjects: List<SubjectEntity>): List<Long>

    @Update
    suspend fun update(subject: SubjectEntity)

    @Query("UPDATE subjects SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("UPDATE subjects SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)

    @Query("SELECT * FROM subjects WHERE archived_at IS NULL ORDER BY name ASC")
    fun getActiveSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjectsIncludingArchived(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectById(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): SubjectEntity?

    @Query("SELECT COUNT(*) FROM subjects WHERE archived_at IS NULL")
    suspend fun getSubjectCount(): Int

    @Query("SELECT * FROM subjects WHERE UPPER(TRIM(name)) IN (:names) OR name IN (:names)")
    suspend fun getByNames(names: List<String>): List<SubjectEntity>

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: Long)
}
