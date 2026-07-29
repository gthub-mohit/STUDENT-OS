package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

/**
 * TimetableSlotDao — Room DAO interface for `timetable_slots` table operations.
 */
@Dao
interface TimetableSlotDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(slot: TimetableSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(slots: List<TimetableSlotEntity>): List<Long>

    @Update
    suspend fun update(slot: TimetableSlotEntity)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM timetable_slots")
    suspend fun deleteAll()

    @Query("SELECT * FROM timetable_slots ORDER BY day_of_week ASC, start_time ASC")
    fun getAllSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE subject_id = :subjectId ORDER BY day_of_week ASC, start_time ASC")
    fun getSlotsForSubject(subjectId: Long): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE day_of_week = :dayOfWeek AND (week_parity IS NULL OR week_parity = :parity) ORDER BY start_time ASC")
    fun getSlotsForDay(dayOfWeek: Int, parity: String?): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE valid_from <= :epochMs AND (valid_until IS NULL OR valid_until >= :epochMs) AND (week_parity IS NULL OR week_parity = :parity)")
    suspend fun getActiveSlotsOnDate(epochMs: Long, parity: String?): List<TimetableSlotEntity>
}
