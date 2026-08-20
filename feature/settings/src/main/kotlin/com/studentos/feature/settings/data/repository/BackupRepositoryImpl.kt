package com.studentos.feature.settings.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.studentos.core.database.AppDatabase
import com.studentos.feature.settings.domain.model.*
import com.studentos.feature.settings.domain.repository.BackupRepository
import com.studentos.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun createBackup(): StudentOsBackup = withContext(Dispatchers.IO) {
        val db = appDatabase.openHelper.readableDatabase

        // 1. Settings (exclude sensitive API key)
        val settingsList = mutableListOf<BackupSetting>()
        db.query(SimpleSQLiteQuery("SELECT key, value FROM settings WHERE key != '${SettingsRepository.KEY_DEEPSEEK_API_KEY}'")).use { c: Cursor ->
            val kIdx = c.getColumnIndex("key")
            val vIdx = c.getColumnIndex("value")
            while (c.moveToNext()) {
                if (kIdx != -1 && vIdx != -1) {
                    settingsList.add(BackupSetting(c.getString(kIdx), c.getString(vIdx)))
                }
            }
        }

        // 2. Subjects
        val subjectsList = mutableListOf<BackupSubject>()
        db.query(SimpleSQLiteQuery("SELECT id, name, archived_at FROM subjects")).use { c: Cursor ->
            val idIdx = c.getColumnIndex("id")
            val nameIdx = c.getColumnIndex("name")
            val archIdx = c.getColumnIndex("archived_at")
            while (c.moveToNext()) {
                val arch = if (c.isNull(archIdx)) null else c.getLong(archIdx)
                subjectsList.add(BackupSubject(c.getLong(idIdx), c.getString(nameIdx), arch))
            }
        }

        // 3. Timetable Slots
        val slotsList = mutableListOf<BackupTimetableSlot>()
        db.query(SimpleSQLiteQuery("SELECT id, subject_id, day_of_week, start_time, end_time, location, week_parity, valid_from, valid_until FROM timetable_slots")).use { c: Cursor ->
            while (c.moveToNext()) {
                slotsList.add(
                    BackupTimetableSlot(
                        id = c.getLong(0),
                        subjectId = c.getLong(1),
                        dayOfWeek = c.getInt(2),
                        startTime = c.getString(3),
                        endTime = c.getString(4),
                        location = if (c.isNull(5)) null else c.getString(5),
                        weekParity = if (c.isNull(6)) null else c.getString(6),
                        validFrom = c.getLong(7),
                        validUntil = if (c.isNull(8)) null else c.getLong(8)
                    )
                )
            }
        }

        // 4. Class Events
        val eventsList = mutableListOf<BackupClassEvent>()
        db.query(SimpleSQLiteQuery("SELECT id, timetable_slot_id, subject_id, scheduled_at, status, updated_at, linked_slot_id FROM class_events")).use { c: Cursor ->
            while (c.moveToNext()) {
                eventsList.add(
                    BackupClassEvent(
                        id = c.getLong(0),
                        timetableSlotId = if (c.isNull(1)) null else c.getLong(1),
                        subjectId = c.getLong(2),
                        scheduledAt = c.getLong(3),
                        status = c.getString(4),
                        updatedAt = c.getLong(5),
                        linkedSlotId = if (c.isNull(6)) null else c.getLong(6)
                    )
                )
            }
        }

        // 5. Assignments
        val assignmentsList = mutableListOf<BackupAssignment>()
        db.query(SimpleSQLiteQuery("SELECT id, subject_id, title, notes, deadline, priority, status, task_type, reminder_lead_ms, attachment_uri, updated_at FROM assignments")).use { c: Cursor ->
            while (c.moveToNext()) {
                assignmentsList.add(
                    BackupAssignment(
                        id = c.getLong(0),
                        subjectId = c.getLong(1),
                        title = c.getString(2),
                        notes = if (c.isNull(3)) null else c.getString(3),
                        deadline = c.getLong(4),
                        priority = c.getString(5),
                        status = c.getString(6),
                        taskType = if (c.isNull(7)) "ASSIGNMENT" else c.getString(7),
                        reminderLeadMs = if (c.isNull(8)) null else c.getLong(8),
                        attachmentUri = if (c.isNull(9)) null else c.getString(9),
                        updatedAt = c.getLong(10)
                    )
                )
            }
        }

        // 6. CP Profiles
        val cpProfilesList = mutableListOf<BackupCpProfile>()
        db.query(SimpleSQLiteQuery("SELECT id, platform, handle, current_rating, highest_rating, rank, problems_solved, contest_count, last_synced_at FROM cp_profiles")).use { c: Cursor ->
            while (c.moveToNext()) {
                cpProfilesList.add(
                    BackupCpProfile(
                        id = c.getLong(0),
                        platform = c.getString(1),
                        handle = c.getString(2),
                        currentRating = if (c.isNull(3)) null else c.getInt(3),
                        highestRating = if (c.isNull(4)) null else c.getInt(4),
                        rank = if (c.isNull(5)) null else c.getString(5),
                        problemsSolved = if (c.isNull(6)) null else c.getInt(6),
                        contestCount = if (c.isNull(7)) null else c.getInt(7),
                        lastSyncedAt = if (c.isNull(8)) null else c.getLong(8)
                    )
                )
            }
        }

        // 7. CP Contests
        val cpContestsList = mutableListOf<BackupCpContest>()
        db.query(SimpleSQLiteQuery("SELECT id, profile_id, contest_name, contest_date, rank, rating_change, new_rating, problems_solved FROM cp_contests")).use { c: Cursor ->
            while (c.moveToNext()) {
                cpContestsList.add(
                    BackupCpContest(
                        id = c.getLong(0),
                        profileId = c.getLong(1),
                        contestName = c.getString(2),
                        contestDate = c.getLong(3),
                        rank = if (c.isNull(4)) null else c.getInt(4),
                        ratingChange = if (c.isNull(5)) null else c.getInt(5),
                        newRating = if (c.isNull(6)) null else c.getInt(6),
                        problemsSolved = if (c.isNull(7)) null else c.getInt(7)
                    )
                )
            }
        }

        // 8. CP Reflections
        val cpReflectionsList = mutableListOf<BackupCpReflection>()
        db.query(SimpleSQLiteQuery("SELECT id, contest_id, went_wrong, to_revise, self_rating FROM cp_reflections")).use { c: Cursor ->
            while (c.moveToNext()) {
                cpReflectionsList.add(
                    BackupCpReflection(
                        id = c.getLong(0),
                        contestId = c.getLong(1),
                        wentWrong = if (c.isNull(2)) null else c.getString(2),
                        toRevise = if (c.isNull(3)) null else c.getString(3),
                        selfRating = c.getInt(4)
                    )
                )
            }
        }

        // 9. DSA Categories
        val dsaCategoriesList = mutableListOf<BackupDsaCategory>()
        db.query(SimpleSQLiteQuery("SELECT id, name, sort_order FROM dsa_categories")).use { c: Cursor ->
            while (c.moveToNext()) {
                dsaCategoriesList.add(
                    BackupDsaCategory(
                        id = c.getLong(0),
                        name = c.getString(1),
                        sortOrder = c.getInt(2)
                    )
                )
            }
        }

        // 10. DSA Topics
        val dsaTopicsList = mutableListOf<BackupDsaTopic>()
        db.query(SimpleSQLiteQuery("SELECT id, category_id, name, difficulty, confidence_level, revision_status, next_revision_date, notes, updated_at FROM dsa_topics")).use { c: Cursor ->
            while (c.moveToNext()) {
                dsaTopicsList.add(
                    BackupDsaTopic(
                        id = c.getLong(0),
                        categoryId = c.getLong(1),
                        name = c.getString(2),
                        difficulty = c.getString(3),
                        confidenceLevel = c.getInt(4),
                        revisionStatus = c.getString(5),
                        nextRevisionDate = if (c.isNull(6)) null else c.getLong(6),
                        notes = if (c.isNull(7)) null else c.getString(7),
                        updatedAt = c.getLong(8)
                    )
                )
            }
        }

        // 11. Projects
        val projectsList = mutableListOf<BackupProject>()
        db.query(SimpleSQLiteQuery("SELECT id, title, archived_at, inactivity_threshold_days, last_activity_at FROM projects")).use { c: Cursor ->
            while (c.moveToNext()) {
                projectsList.add(
                    BackupProject(
                        id = c.getLong(0),
                        title = c.getString(1),
                        archivedAt = if (c.isNull(2)) null else c.getLong(2),
                        inactivityThresholdDays = c.getInt(3),
                        lastActivityAt = c.getLong(4)
                    )
                )
            }
        }

        // 12. Milestones
        val milestonesList = mutableListOf<BackupMilestone>()
        db.query(SimpleSQLiteQuery("SELECT id, project_id, target_date, title, description, status FROM milestones")).use { c: Cursor ->
            while (c.moveToNext()) {
                milestonesList.add(
                    BackupMilestone(
                        id = c.getLong(0),
                        projectId = c.getLong(1),
                        targetDate = if (c.isNull(2)) null else c.getLong(2),
                        title = c.getString(3),
                        description = if (c.isNull(4)) null else c.getString(4),
                        status = c.getString(5)
                    )
                )
            }
        }

        // 13. Bugs
        val bugsList = mutableListOf<BackupBug>()
        db.query(SimpleSQLiteQuery("SELECT id, project_id, description, severity, status FROM bugs")).use { c: Cursor ->
            while (c.moveToNext()) {
                bugsList.add(
                    BackupBug(
                        id = c.getLong(0),
                        projectId = c.getLong(1),
                        description = c.getString(2),
                        severity = c.getString(3),
                        status = c.getString(4)
                    )
                )
            }
        }

        // 14. Project Tasks
        val projectTasksList = mutableListOf<BackupProjectTask>()
        db.query(SimpleSQLiteQuery("SELECT id, project_id, title, is_next_action, is_parallel, completed_at, sort_order, dependency_task_id, priority, deadline FROM project_tasks")).use { c: Cursor ->
            while (c.moveToNext()) {
                projectTasksList.add(
                    BackupProjectTask(
                        id = c.getLong(0),
                        projectId = c.getLong(1),
                        title = c.getString(2),
                        isNextAction = c.getInt(3) == 1,
                        isParallel = c.getInt(4) == 1,
                        completedAt = if (c.isNull(5)) null else c.getLong(5),
                        sortOrder = c.getInt(6),
                        dependencyTaskId = if (c.isNull(7)) null else c.getLong(7),
                        priority = if (c.isNull(8)) "MEDIUM" else c.getString(8),
                        deadline = if (c.isNull(9)) null else c.getLong(9)
                    )
                )
            }
        }

        // 15. Project Resources
        val projectResourcesList = mutableListOf<BackupProjectResource>()
        db.query(SimpleSQLiteQuery("SELECT id, project_id, title, url, type FROM project_resources")).use { c: Cursor ->
            while (c.moveToNext()) {
                projectResourcesList.add(
                    BackupProjectResource(
                        id = c.getLong(0),
                        projectId = c.getLong(1),
                        title = c.getString(2),
                        url = c.getString(3),
                        type = c.getString(4)
                    )
                )
            }
        }

        // 16. Daily Briefs
        val dailyBriefsList = mutableListOf<BackupDailyBrief>()
        db.query(SimpleSQLiteQuery("SELECT id, date, snapshot_hash, score_target, score_actual, llm_guidance, guidance_source, guidance_updated_at, generated_at FROM daily_briefs")).use { c: Cursor ->
            while (c.moveToNext()) {
                dailyBriefsList.add(
                    BackupDailyBrief(
                        id = c.getLong(0),
                        date = c.getString(1),
                        snapshotHash = c.getString(2),
                        scoreTarget = c.getInt(3),
                        scoreActual = c.getInt(4),
                        llmGuidance = if (c.isNull(5)) null else c.getString(5),
                        guidanceSource = c.getString(6),
                        guidanceUpdatedAt = c.getLong(7),
                        generatedAt = c.getLong(8)
                    )
                )
            }
        }

        StudentOsBackup(
            version = 1,
            appVersion = "1.0",
            exportedAt = System.currentTimeMillis(),
            settings = settingsList,
            subjects = subjectsList,
            timetableSlots = slotsList,
            classEvents = eventsList,
            assignments = assignmentsList,
            cpProfiles = cpProfilesList,
            cpContests = cpContestsList,
            cpReflections = cpReflectionsList,
            dsaCategories = dsaCategoriesList,
            dsaTopics = dsaTopicsList,
            projects = projectsList,
            milestones = milestonesList,
            bugs = bugsList,
            projectTasks = projectTasksList,
            projectResources = projectResourcesList,
            dailyBriefs = dailyBriefsList
        )
    }

    override suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val backup = createBackup()
        json.encodeToString(backup)
    }

    override suspend fun restoreBackup(backup: StudentOsBackup): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            appDatabase.withTransaction {
                val db = appDatabase.openHelper.writableDatabase

                // 1. Wipe existing tables in foreign key safe dependency order
                db.execSQL("DELETE FROM class_events")
                db.execSQL("DELETE FROM timetable_slots")
                db.execSQL("DELETE FROM assignments")
                db.execSQL("DELETE FROM cp_reflections")
                db.execSQL("DELETE FROM cp_contests")
                db.execSQL("DELETE FROM cp_profiles")
                db.execSQL("DELETE FROM dsa_topics")
                db.execSQL("DELETE FROM dsa_categories")
                db.execSQL("DELETE FROM project_tasks")
                db.execSQL("DELETE FROM milestones")
                db.execSQL("DELETE FROM bugs")
                db.execSQL("DELETE FROM project_resources")
                db.execSQL("DELETE FROM projects")
                db.execSQL("DELETE FROM subjects")
                db.execSQL("DELETE FROM daily_briefs")
                db.execSQL("DELETE FROM recommendation_cache")
                db.execSQL("DELETE FROM settings WHERE key != '${SettingsRepository.KEY_DEEPSEEK_API_KEY}'")

                // 2. Insert Settings
                backup.settings.forEach { item ->
                    val cv = ContentValues().apply {
                        put("key", item.key)
                        put("value", item.value)
                    }
                    db.insert("settings", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 3. Insert Subjects
                backup.subjects.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("archived_at", item.archivedAt)
                    }
                    db.insert("subjects", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 4. Insert Timetable Slots
                backup.timetableSlots.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("subject_id", item.subjectId)
                        put("day_of_week", item.dayOfWeek)
                        put("start_time", item.startTime)
                        put("end_time", item.endTime)
                        put("location", item.location)
                        put("week_parity", item.weekParity)
                        put("valid_from", item.validFrom)
                        put("valid_until", item.validUntil)
                    }
                    db.insert("timetable_slots", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 5. Insert Class Events
                backup.classEvents.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("timetable_slot_id", item.timetableSlotId)
                        put("subject_id", item.subjectId)
                        put("scheduled_at", item.scheduledAt)
                        put("status", item.status)
                        put("updated_at", item.updatedAt)
                        put("linked_slot_id", item.linkedSlotId)
                    }
                    db.insert("class_events", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 6. Insert Assignments
                backup.assignments.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("subject_id", item.subjectId)
                        put("title", item.title)
                        put("notes", item.notes)
                        put("deadline", item.deadline)
                        put("priority", item.priority)
                        put("status", item.status)
                        put("task_type", item.taskType)
                        put("reminder_lead_ms", item.reminderLeadMs)
                        put("attachment_uri", item.attachmentUri)
                        put("updated_at", item.updatedAt)
                    }
                    db.insert("assignments", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 7. Insert CP Profiles
                backup.cpProfiles.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("platform", item.platform)
                        put("handle", item.handle)
                        put("current_rating", item.currentRating)
                        put("highest_rating", item.highestRating)
                        put("rank", item.rank)
                        put("problems_solved", item.problemsSolved)
                        put("contest_count", item.contestCount)
                        put("last_synced_at", item.lastSyncedAt)
                    }
                    db.insert("cp_profiles", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 8. Insert CP Contests
                backup.cpContests.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("profile_id", item.profileId)
                        put("contest_name", item.contestName)
                        put("contest_date", item.contestDate)
                        put("rank", item.rank)
                        put("rating_change", item.ratingChange)
                        put("new_rating", item.newRating)
                        put("problems_solved", item.problemsSolved)
                    }
                    db.insert("cp_contests", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 9. Insert CP Reflections
                backup.cpReflections.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("contest_id", item.contestId)
                        put("went_wrong", item.wentWrong)
                        put("to_revise", item.toRevise)
                        put("self_rating", item.selfRating)
                    }
                    db.insert("cp_reflections", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 10. Insert DSA Categories
                backup.dsaCategories.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("sort_order", item.sortOrder)
                    }
                    db.insert("dsa_categories", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 11. Insert DSA Topics
                backup.dsaTopics.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("category_id", item.categoryId)
                        put("name", item.name)
                        put("difficulty", item.difficulty)
                        put("confidence_level", item.confidenceLevel)
                        put("revision_status", item.revisionStatus)
                        put("next_revision_date", item.nextRevisionDate)
                        put("notes", item.notes)
                        put("updated_at", item.updatedAt)
                    }
                    db.insert("dsa_topics", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 12. Insert Projects
                backup.projects.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("archived_at", item.archivedAt)
                        put("inactivity_threshold_days", item.inactivityThresholdDays)
                        put("last_activity_at", item.lastActivityAt)
                    }
                    db.insert("projects", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 13. Insert Milestones
                backup.milestones.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("project_id", item.projectId)
                        put("target_date", item.targetDate)
                        put("title", item.title)
                        put("description", item.description)
                        put("status", item.status)
                    }
                    db.insert("milestones", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 14. Insert Bugs
                backup.bugs.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("project_id", item.projectId)
                        put("description", item.description)
                        put("severity", item.severity)
                        put("status", item.status)
                    }
                    db.insert("bugs", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 15. Insert Project Tasks
                backup.projectTasks.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("project_id", item.projectId)
                        put("title", item.title)
                        put("is_next_action", if (item.isNextAction) 1 else 0)
                        put("is_parallel", if (item.isParallel) 1 else 0)
                        put("completed_at", item.completedAt)
                        put("sort_order", item.sortOrder)
                        put("dependency_task_id", item.dependencyTaskId)
                        put("priority", item.priority)
                        put("deadline", item.deadline)
                    }
                    db.insert("project_tasks", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 16. Insert Project Resources
                backup.projectResources.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("project_id", item.projectId)
                        put("title", item.title)
                        put("url", item.url)
                        put("type", item.type)
                    }
                    db.insert("project_resources", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }

                // 17. Insert Daily Briefs
                backup.dailyBriefs.forEach { item ->
                    val cv = ContentValues().apply {
                        put("id", item.id)
                        put("date", item.date)
                        put("snapshot_hash", item.snapshotHash)
                        put("score_target", item.scoreTarget)
                        put("score_actual", item.scoreActual)
                        put("llm_guidance", item.llmGuidance)
                        put("guidance_source", item.guidanceSource)
                        put("guidance_updated_at", item.guidanceUpdatedAt)
                        put("generated_at", item.generatedAt)
                    }
                    db.insert("daily_briefs", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }
            }
        }
    }

    override suspend fun restoreBackupJson(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = json.decodeFromString<StudentOsBackup>(jsonString)
            restoreBackup(backup).getOrThrow()
        }
    }
}
