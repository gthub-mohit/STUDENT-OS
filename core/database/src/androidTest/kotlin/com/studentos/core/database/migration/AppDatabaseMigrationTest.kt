package com.studentos.core.database.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.studentos.core.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    private val TEST_DB = "migration-test.db"

    @Test
    fun migration4To5_preservesExistingData_andCreatesNewTables() {
        // 1. Create database in version 4 schema
        var db = helper.createDatabase(TEST_DB, 4)

        // 2. Insert representative legacy records into v4 tables
        val subjectValues = ContentValues().apply {
            put("id", 1)
            put("name", "Embedded Systems")
        }
        db.insert("subjects", SQLiteDatabase.CONFLICT_FAIL, subjectValues)

        val slotValues = ContentValues().apply {
            put("id", 101)
            put("subject_id", 1)
            put("day_of_week", 1)
            put("start_time", "09:00")
            put("end_time", "10:00")
            put("valid_from", 1700000000000L)
        }
        db.insert("timetable_slots", SQLiteDatabase.CONFLICT_FAIL, slotValues)

        val classEventValues = ContentValues().apply {
            put("id", 501)
            put("slot_id", 101)
            put("event_date", "2026-10-18")
            put("status", "PRESENT")
            put("recorded_at", 1700000000000L)
        }
        db.insert("class_events", SQLiteDatabase.CONFLICT_FAIL, classEventValues)

        val settingValues = ContentValues().apply {
            put("key", "attendance_threshold")
            put("value", "80")
        }
        db.insert("settings", SQLiteDatabase.CONFLICT_FAIL, settingValues)

        val assignmentValues = ContentValues().apply {
            put("id", 201)
            put("subject_id", 1)
            put("title", "OS Lab Assignment 1")
            put("deadline", 1700050000000L)
            put("priority", "HIGH")
            put("status", "PENDING")
            put("task_type", "ASSIGNMENT")
            put("created_at", 1700000000000L)
            put("updated_at", 1700000000000L)
        }
        db.insert("assignments", SQLiteDatabase.CONFLICT_FAIL, assignmentValues)

        db.close()

        // 3. Migrate from version 4 to version 5
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // 4. Verify that existing v4 records are 100% preserved
        val subjectCursor = db.query("SELECT name FROM subjects WHERE id = 1")
        assertTrue("subjects row must exist", subjectCursor.moveToFirst())
        assertEquals("Embedded Systems", subjectCursor.getString(0))
        subjectCursor.close()

        val slotCursor = db.query("SELECT start_time, end_time FROM timetable_slots WHERE id = 101")
        assertTrue("timetable_slots row must exist", slotCursor.moveToFirst())
        assertEquals("09:00", slotCursor.getString(0))
        assertEquals("10:00", slotCursor.getString(1))
        slotCursor.close()

        val eventCursor = db.query("SELECT status FROM class_events WHERE id = 501")
        assertTrue("class_events row must exist", eventCursor.moveToFirst())
        assertEquals("PRESENT", eventCursor.getString(0))
        eventCursor.close()

        val settingCursor = db.query("SELECT value FROM settings WHERE key = 'attendance_threshold'")
        assertTrue("settings row must exist", settingCursor.moveToFirst())
        assertEquals("80", settingCursor.getString(0))
        settingCursor.close()

        val assignCursor = db.query("SELECT title, priority FROM assignments WHERE id = 201")
        assertTrue("assignments row must exist", assignCursor.moveToFirst())
        assertEquals("OS Lab Assignment 1", assignCursor.getString(0))
        assertEquals("HIGH", assignCursor.getString(1))
        assignCursor.close()

        // 5. Verify operations on the new v5 tables
        val goalValues = ContentValues().apply {
            put("title", "Solve 200 DSA Problems")
            put("category", "DSA")
            put("metric_type", "COUNT")
            put("unit", "questions")
            put("start_value", 45.0)
            put("target_value", 200.0)
            put("start_date", 1700000000000L)
            put("target_date", 1735689600000L)
            put("is_archived", 0)
            put("created_at", 1700000000000L)
            put("updated_at", 1700000000000L)
        }
        val goalId = db.insert("goals", SQLiteDatabase.CONFLICT_FAIL, goalValues)
        assertTrue("goal insert must succeed", goalId > 0)

        val commitmentValues = ContentValues().apply {
            put("title", "Finish STM32 SPI Driver")
            put("category", "PROJECT")
            put("deadline", 1700080000000L)
            put("priority", "HIGH")
            put("status", "PENDING")
            put("estimated_minutes", 120)
            put("notes", "Use DMA for large transfers")
            put("created_at", 1700000000000L)
        }
        val commitmentId = db.insert("commitments", SQLiteDatabase.CONFLICT_FAIL, commitmentValues)
        assertTrue("commitment insert must succeed", commitmentId > 0)

        val activityValues = ContentValues().apply {
            put("goal_id", goalId)
            put("commitment_id", commitmentId)
            put("timestamp", 1700010000000L)
            put("metric_delta", 5.0)
            put("activity_type", "DSA")
            put("summary", "Solved 5 LeetCode DP questions")
            put("raw_input", "Solved 5 DP questions")
            put("duration_minutes", 90)
            put("confirmed_at", 1700010000000L)
        }
        val activityId = db.insert("activity_logs", SQLiteDatabase.CONFLICT_FAIL, activityValues)
        assertTrue("activity_log insert must succeed", activityId > 0)

        val journalValues = ContentValues().apply {
            put("date", "2026-10-18")
            put("raw_log", "Good progress on DSA and STM32")
            put("structured_summary", "Completed 5 DP questions")
            put("created_at", 1700010000000L)
            put("updated_at", 1700010000000L)
        }
        val journalId = db.insert("journal_entries", SQLiteDatabase.CONFLICT_FAIL, journalValues)
        assertTrue("journal_entry insert must succeed", journalId > 0)

        // 6. Verify foreign key ON DELETE SET NULL on activity_logs
        db.delete("goals", "id = ?", arrayOf(goalId.toString()))
        val logCursor = db.query("SELECT goal_id, summary FROM activity_logs WHERE id = $activityId")
        assertTrue("activity log must still exist after goal deletion", logCursor.moveToFirst())
        assertNull("goal_id must be set to NULL when parent goal is deleted", logCursor.getString(0))
        assertEquals("Solved 5 LeetCode DP questions", logCursor.getString(1))
        logCursor.close()

        db.close()
    }

    @Test
    fun migration3To5_viaCompositeMigration_succeeds() {
        var db = helper.createDatabase(TEST_DB, 3)
        val settingValues = ContentValues().apply {
            put("key", "test_key")
            put("value", "test_val")
        }
        db.insert("settings", SQLiteDatabase.CONFLICT_FAIL, settingValues)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_3_5)
        val cursor = db.query("SELECT value FROM settings WHERE key = 'test_key'")
        assertTrue(cursor.moveToFirst())
        assertEquals("test_val", cursor.getString(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migration1To5_viaCompositeMigration_succeeds() {
        var db = helper.createDatabase(TEST_DB, 1)
        val subjectValues = ContentValues().apply {
            put("name", "Legacy Math")
        }
        db.insert("subjects", SQLiteDatabase.CONFLICT_FAIL, subjectValues)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_1_5)
        val cursor = db.query("SELECT name FROM subjects WHERE name = 'Legacy Math'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Legacy Math", cursor.getString(0))
        cursor.close()
        db.close()
    }
}
