package com.studentos.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private fun safeAddColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String, columnDef: String) {
    try {
        val cursor = db.query("PRAGMA table_info($tableName)")
        var exists = false
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex != -1 && cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                exists = true
                break
            }
        }
        cursor.close()
        if (!exists) {
            db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDef")
        }
    } catch (_: Throwable) {
        // Ignored if column already exists
    }
}

private fun migrateProjectTasksToV4(db: SupportSQLiteDatabase) {
    try {
        try {
            db.execSQL("DROP INDEX IF EXISTS `idx_one_next_action`")
        } catch (_: Throwable) {}

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `project_tasks_temp_v4` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `project_id` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `is_next_action` INTEGER NOT NULL DEFAULT 0,
                `is_parallel` INTEGER NOT NULL DEFAULT 0,
                `completed_at` INTEGER,
                `sort_order` INTEGER NOT NULL DEFAULT 0,
                `dependency_task_id` INTEGER,
                `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                `deadline` INTEGER,
                FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        // Inspect existing project_tasks columns
        val cursor = db.query("PRAGMA table_info(project_tasks)")
        val columns = mutableSetOf<String>()
        val nameIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIdx != -1) {
                columns.add(cursor.getString(nameIdx).lowercase())
            }
        }
        cursor.close()

        val depCol = if (columns.contains("dependency_task_id")) "`dependency_task_id`" else "NULL"
        val priorityCol = if (columns.contains("priority")) "COALESCE(`priority`, 'MEDIUM')" else "'MEDIUM'"
        val deadlineCol = if (columns.contains("deadline")) "`deadline`" else "NULL"
        val isParallelCol = if (columns.contains("is_parallel")) "COALESCE(`is_parallel`, 0)" else "0"
        val isNextCol = if (columns.contains("is_next_action")) "COALESCE(`is_next_action`, 0)" else "0"
        val sortOrderCol = if (columns.contains("sort_order")) "COALESCE(`sort_order`, 0)" else "0"
        val completedAtCol = if (columns.contains("completed_at")) "`completed_at`" else "NULL"

        db.execSQL("""
            INSERT INTO `project_tasks_temp_v4` (
                `id`, `project_id`, `title`, `is_next_action`, `is_parallel`, 
                `completed_at`, `sort_order`, `dependency_task_id`, `priority`, `deadline`
            )
            SELECT 
                `id`, `project_id`, `title`, $isNextCol, $isParallelCol, 
                $completedAtCol, $sortOrderCol, $depCol, $priorityCol, $deadlineCol
            FROM `project_tasks`
        """.trimIndent())

        db.execSQL("DROP TABLE IF EXISTS `project_tasks`")
        db.execSQL("ALTER TABLE `project_tasks_temp_v4` RENAME TO `project_tasks`")

        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_tasks_project_next` ON `project_tasks` (`project_id`, `is_next_action`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_tasks_dependency` ON `project_tasks` (`dependency_task_id`)")
    } catch (_: Throwable) {
        // Fallback: If table recreation encounters any unexpected SQLite issue, attempt direct ALTERs
        safeAddColumn(db, "project_tasks", "dependency_task_id", "INTEGER")
        safeAddColumn(db, "project_tasks", "priority", "TEXT NOT NULL DEFAULT 'MEDIUM'")
        safeAddColumn(db, "project_tasks", "deadline", "INTEGER")
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_dependency ON project_tasks(dependency_task_id)")
        } catch (_: Throwable) {}
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "cp_profiles", "highest_rating", "INTEGER")
        safeAddColumn(db, "cp_profiles", "rank", "TEXT")
        safeAddColumn(db, "cp_profiles", "problems_solved", "INTEGER")
        safeAddColumn(db, "cp_profiles", "contest_count", "INTEGER")

        safeAddColumn(db, "dsa_topics", "difficulty", "TEXT NOT NULL DEFAULT 'MEDIUM'")
        safeAddColumn(db, "dsa_topics", "next_revision_date", "INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "assignments", "task_type", "TEXT NOT NULL DEFAULT 'ASSIGNMENT'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateProjectTasksToV4(db)
    }
}

val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2.migrate(db)
        MIGRATION_2_3.migrate(db)
    }
}

val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)
    }
}

val MIGRATION_1_4 = object : Migration(1, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2.migrate(db)
        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 0. Clean up any unmanaged partial index on project_tasks so Room schema validation succeeds
        try {
            db.execSQL("DROP INDEX IF EXISTS `idx_one_next_action`")
        } catch (_: Throwable) {}

        // 1. Create goals table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `metric_type` TEXT NOT NULL,
                `unit` TEXT NOT NULL,
                `start_value` REAL NOT NULL,
                `target_value` REAL NOT NULL,
                `start_date` INTEGER NOT NULL,
                `target_date` INTEGER NOT NULL,
                `is_archived` INTEGER NOT NULL DEFAULT 0,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_goals_active` ON `goals` (`is_archived`, `target_date`)")

        // 2. Create commitments table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `commitments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `deadline` INTEGER,
                `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `estimated_minutes` INTEGER,
                `notes` TEXT,
                `created_at` INTEGER NOT NULL,
                `completed_at` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_commitments_deadline` ON `commitments` (`status`, `deadline`)")

        // 3. Create activity_logs table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `activity_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `goal_id` INTEGER,
                `commitment_id` INTEGER,
                `timestamp` INTEGER NOT NULL,
                `metric_delta` REAL NOT NULL,
                `activity_type` TEXT NOT NULL,
                `summary` TEXT NOT NULL,
                `raw_input` TEXT,
                `duration_minutes` INTEGER,
                `dedup_hash` TEXT,
                `confirmed_at` INTEGER NOT NULL,
                FOREIGN KEY(`goal_id`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`commitment_id`) REFERENCES `commitments`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_activity_goal` ON `activity_logs` (`goal_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_activity_commitment` ON `activity_logs` (`commitment_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_activity_timestamp` ON `activity_logs` (`timestamp`)")

        // 4. Create journal_entries table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `journal_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `raw_log` TEXT NOT NULL,
                `structured_summary` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_journal_date` ON `journal_entries` (`date`)")
    }
}

val MIGRATION_3_5 = object : Migration(3, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_3_4.migrate(db)
        MIGRATION_4_5.migrate(db)
    }
}

val MIGRATION_2_5 = object : Migration(2, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_2_4.migrate(db)
        MIGRATION_4_5.migrate(db)
    }
}

val MIGRATION_1_5 = object : Migration(1, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_4.migrate(db)
        MIGRATION_4_5.migrate(db)
    }
}

