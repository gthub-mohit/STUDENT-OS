package com.studentos.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.DsaCategoryEntity
import com.studentos.core.database.entity.DsaTopicEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ForeignKeyConstraintTest — Instrumented test suite verifying `ON DELETE RESTRICT` foreign key enforcement.
 */
@RunWith(AndroidJUnit4::class)
class ForeignKeyConstraintTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test(expected = SQLiteConstraintException::class)
    fun subjectDeleteFails_whenTimetableSlotExists() = runBlocking {
        val subjectId = database.subjectDao().insert(SubjectEntity(name = "Mathematics"))
        database.timetableSlotDao().insert(
            TimetableSlotEntity(
                subjectId = subjectId,
                dayOfWeek = 1,
                startTime = "09:00",
                endTime = "10:00",
                location = "Room 101"
            )
        )

        // Attempting to delete subject when an active timetable_slot references it must throw SQLiteConstraintException
        database.subjectDao().archive(subjectId, System.currentTimeMillis())
        // Hard deletion attempt to test RESTRICT trigger
        database.openHelper.writableDatabase.execSQL("DELETE FROM subjects WHERE id = $subjectId")
    }

    @Test(expected = SQLiteConstraintException::class)
    fun subjectDeleteFails_whenAssignmentExists() = runBlocking {
        val subjectId = database.subjectDao().insert(SubjectEntity(name = "Physics"))
        database.assignmentDao().insert(
            AssignmentEntity(
                subjectId = subjectId,
                title = "Lab Report 1",
                deadline = System.currentTimeMillis() + 86400000L,
                createdAt = System.currentTimeMillis()
            )
        )

        // Hard deletion attempt to test RESTRICT constraint on assignments -> subjects(id)
        database.openHelper.writableDatabase.execSQL("DELETE FROM subjects WHERE id = $subjectId")
    }

    @Test(expected = SQLiteConstraintException::class)
    fun dsaCategoryDeleteFails_whenTopicExists() = runBlocking {
        val categoryId = database.dsaCategoryDao().insert(DsaCategoryEntity(name = "Binary Search Trees"))
        database.dsaTopicDao().insert(
            DsaTopicEntity(
                categoryId = categoryId,
                name = "BST Inorder Traversal"
            )
        )

        // Deleting category when active topic exists must throw SQLiteConstraintException
        database.dsaCategoryDao().deleteById(categoryId)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun timetableSlotDeleteFails_whenClassEventExists() = runBlocking {
        val subjectId = database.subjectDao().insert(SubjectEntity(name = "Chemistry"))
        val slotId = database.timetableSlotDao().insert(
            TimetableSlotEntity(
                subjectId = subjectId,
                dayOfWeek = 2,
                startTime = "10:00",
                endTime = "11:00",
                location = "Lab 3"
            )
        )
        database.classEventDao().insert(
            ClassEventEntity(
                timetableSlotId = slotId,
                subjectId = subjectId,
                scheduledAt = System.currentTimeMillis(),
                endAt = System.currentTimeMillis() + 3600000L
            )
        )

        // Deleting slot when active class event references it must throw SQLiteConstraintException
        database.timetableSlotDao().deleteById(slotId)
    }
}
