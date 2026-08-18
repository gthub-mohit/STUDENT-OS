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

val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2.migrate(db)
        MIGRATION_2_3.migrate(db)
    }
}
