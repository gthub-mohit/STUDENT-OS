package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import kotlinx.coroutines.flow.Flow

/**
 * ClassEventDao — Room DAO interface for `class_events` table operations.
 */
@Dao
interface ClassEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: ClassEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(events: List<ClassEventEntity>): List<Long>

    @Query("UPDATE class_events SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)

    @Query("UPDATE class_events SET end_at = :endAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateEndAt(id: Long, endAt: Long, updatedAt: Long)

    @Query("SELECT * FROM class_events WHERE id = :id")
    suspend fun getEventByIdOnce(id: Long): ClassEventEntity?

    @Query("SELECT * FROM class_events WHERE scheduled_at >= :startEpoch AND scheduled_at <= :endEpoch ORDER BY scheduled_at ASC")
    fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>>

    @Query("SELECT * FROM class_events WHERE subject_id = :subjectId ORDER BY scheduled_at ASC")
    fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>>

    @Query("SELECT * FROM class_events WHERE scheduled_at >= :startEpoch AND scheduled_at <= :endEpoch ORDER BY scheduled_at ASC")
    fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>>

    @Query("SELECT COUNT(*) FROM class_events WHERE subject_id = :subjectId AND status = :status")
    suspend fun countByStatus(subjectId: Long, status: String): Int

    @Query("""
        SELECT 
            s.id AS subjectId,
            s.name AS subjectName,
            SUM(CASE WHEN ce.status = 'PRESENT' THEN 1 ELSE 0 END) AS presentCount,
            SUM(CASE WHEN ce.status = 'ABSENT' THEN 1 ELSE 0 END) AS absentCount,
            SUM(CASE WHEN ce.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledCount,
            SUM(CASE WHEN ce.status = 'HOLIDAY' THEN 1 ELSE 0 END) AS holidayCount,
            SUM(CASE WHEN ce.status = 'EXTRA_CLASS' THEN 1 ELSE 0 END) AS extraPresentCount,
            SUM(CASE WHEN ce.status IN ('PRESENT', 'ABSENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) AS totalHeldCount,
            CASE 
                WHEN SUM(CASE WHEN ce.status IN ('PRESENT', 'ABSENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) = 0 THEN 0.0
                ELSE (CAST(SUM(CASE WHEN ce.status IN ('PRESENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) AS REAL) / 
                      CAST(SUM(CASE WHEN ce.status IN ('PRESENT', 'ABSENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) AS REAL)) * 100.0
            END AS percentage
        FROM subjects s
        LEFT JOIN class_events ce ON s.id = ce.subject_id
        WHERE s.id = :subjectId
        GROUP BY s.id
    """)
    fun getAttendanceSummary(subjectId: Long): Flow<SubjectAttendanceSummary?>

    @Query("""
        SELECT 
            s.id AS subjectId,
            s.name AS subjectName,
            SUM(CASE WHEN ce.status = 'PRESENT' THEN 1 ELSE 0 END) AS presentCount,
            SUM(CASE WHEN ce.status = 'ABSENT' THEN 1 ELSE 0 END) AS absentCount,
            SUM(CASE WHEN ce.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledCount,
            SUM(CASE WHEN ce.status = 'HOLIDAY' THEN 1 ELSE 0 END) AS holidayCount,
            SUM(CASE WHEN ce.status = 'EXTRA_CLASS' THEN 1 ELSE 0 END) AS extraPresentCount,
            SUM(CASE WHEN ce.status IN ('PRESENT', 'ABSENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) AS totalHeldCount,
            CASE 
                WHEN SUM(CASE WHEN ce.status IN ('PRESENT', 'ABSENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) = 0 THEN 0.0
                ELSE (CAST(SUM(CASE WHEN ce.status IN ('PRESENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) AS REAL) / 
                      CAST(SUM(CASE WHEN ce.status IN ('PRESENT', 'ABSENT', 'EXTRA_CLASS') THEN 1 ELSE 0 END) AS REAL)) * 100.0
            END AS percentage
        FROM subjects s
        LEFT JOIN class_events ce ON s.id = ce.subject_id
        WHERE s.archived_at IS NULL
        GROUP BY s.id
        ORDER BY s.name ASC
    """)
    fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>>
}
