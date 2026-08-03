package com.studentos.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cp_profiles ADD COLUMN highest_rating INTEGER")
        db.execSQL("ALTER TABLE cp_profiles ADD COLUMN rank TEXT")
        db.execSQL("ALTER TABLE cp_profiles ADD COLUMN problems_solved INTEGER")
        db.execSQL("ALTER TABLE cp_profiles ADD COLUMN contest_count INTEGER")

        db.execSQL("ALTER TABLE dsa_topics ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'MEDIUM'")
        db.execSQL("ALTER TABLE dsa_topics ADD COLUMN next_revision_date INTEGER")
    }
}
