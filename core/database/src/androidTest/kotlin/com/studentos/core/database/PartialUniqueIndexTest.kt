package com.studentos.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.database.entity.ProjectTaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PartialUniqueIndexTest — Instrumented test suite verifying `idx_one_next_action` partial unique index invariant.
 */
@RunWith(AndroidJUnit4::class)
class PartialUniqueIndexTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : Room.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_one_next_action ON project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0")
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_one_next_action ON project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0")
                }
            })
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insertSecondNextActionTaskInSequentialMode_throwsException() = runBlocking {
        val projectId = database.projectDao().insert(
            ProjectEntity(title = "Student OS App", lastActivityAt = System.currentTimeMillis())
        )

        // Insert first task as next action in sequential mode (is_parallel = 0)
        database.projectTaskDao().insert(
            ProjectTaskEntity(
                projectId = projectId,
                title = "Task 1",
                isNextAction = true,
                isParallel = false
            )
        )

        // Inserting second task as next action in sequential mode must throw SQLiteConstraintException
        database.projectTaskDao().insert(
            ProjectTaskEntity(
                projectId = projectId,
                title = "Task 2",
                isNextAction = true,
                isParallel = false
            )
        )
    }

    @Test
    fun insertMultipleNextActionTasksInParallelMode_succeeds() = runBlocking {
        val projectId = database.projectDao().insert(
            ProjectEntity(title = "Parallel Research", lastActivityAt = System.currentTimeMillis())
        )

        // Insert first task in parallel mode (is_parallel = 1)
        val id1 = database.projectTaskDao().insert(
            ProjectTaskEntity(
                projectId = projectId,
                title = "Parallel Task 1",
                isNextAction = true,
                isParallel = true
            )
        )

        // Insert second task in parallel mode (is_parallel = 1)
        val id2 = database.projectTaskDao().insert(
            ProjectTaskEntity(
                projectId = projectId,
                title = "Parallel Task 2",
                isNextAction = true,
                isParallel = true
            )
        )

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
    }

    @Test
    fun insertNextActionTasksInDifferentProjects_succeeds() = runBlocking {
        val project1Id = database.projectDao().insert(
            ProjectEntity(title = "Project 1", lastActivityAt = System.currentTimeMillis())
        )
        val project2Id = database.projectDao().insert(
            ProjectEntity(title = "Project 2", lastActivityAt = System.currentTimeMillis())
        )

        val id1 = database.projectTaskDao().insert(
            ProjectTaskEntity(
                projectId = project1Id,
                title = "P1 Next Action",
                isNextAction = true,
                isParallel = false
            )
        )
        val id2 = database.projectTaskDao().insert(
            ProjectTaskEntity(
                projectId = project2Id,
                title = "P2 Next Action",
                isNextAction = true,
                isParallel = false
            )
        )

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
    }
}
