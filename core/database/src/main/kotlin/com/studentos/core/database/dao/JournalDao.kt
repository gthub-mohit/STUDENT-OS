package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * JournalDao — Room DAO interface for `journal_entries` table operations.
 */
@Dao
interface JournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun observeEntryByDate(date: String): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Query("UPDATE journal_entries SET structured_summary = :summary, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSummary(id: Long, summary: String?, updatedAt: Long)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
