package com.studentos.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studentos.core.database.AppDatabase
import com.studentos.core.database.dao.AiCallLogDao
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.BugDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.CpReflectionDao
import com.studentos.core.database.dao.DailyBriefDao
import com.studentos.core.database.dao.DsaCategoryDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.MilestoneDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.ProjectResourceDao
import com.studentos.core.database.dao.ProjectTaskDao
import com.studentos.core.database.dao.RecommendationCacheDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.migration.MIGRATION_1_2
import com.studentos.core.database.migration.MIGRATION_2_3
import com.studentos.core.database.migration.MIGRATION_1_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DatabaseModule — Hilt module providing singletons for Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // Enable Write-Ahead Logging (WAL mode) for concurrency and performance
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    try {
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_one_next_action ON project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0")
                    } catch (_: Throwable) {}
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    try {
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_one_next_action ON project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0")
                    } catch (_: Throwable) {}
                }
            })
            .build()
    }

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()

    @Provides
    fun provideSubjectDao(database: AppDatabase): SubjectDao = database.subjectDao()

    @Provides
    fun provideTimetableSlotDao(database: AppDatabase): TimetableSlotDao = database.timetableSlotDao()

    @Provides
    fun provideClassEventDao(database: AppDatabase): ClassEventDao = database.classEventDao()

    @Provides
    fun provideAssignmentDao(database: AppDatabase): AssignmentDao = database.assignmentDao()

    @Provides
    fun provideCpProfileDao(database: AppDatabase): CpProfileDao = database.cpProfileDao()

    @Provides
    fun provideCpContestDao(database: AppDatabase): CpContestDao = database.cpContestDao()

    @Provides
    fun provideCpReflectionDao(database: AppDatabase): CpReflectionDao = database.cpReflectionDao()

    @Provides
    fun provideDsaCategoryDao(database: AppDatabase): DsaCategoryDao = database.dsaCategoryDao()

    @Provides
    fun provideDsaTopicDao(database: AppDatabase): DsaTopicDao = database.dsaTopicDao()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideProjectTaskDao(database: AppDatabase): ProjectTaskDao = database.projectTaskDao()

    @Provides
    fun provideMilestoneDao(database: AppDatabase): MilestoneDao = database.milestoneDao()

    @Provides
    fun provideBugDao(database: AppDatabase): BugDao = database.bugDao()

    @Provides
    fun provideProjectResourceDao(database: AppDatabase): ProjectResourceDao = database.projectResourceDao()

    @Provides
    fun provideDailyBriefDao(database: AppDatabase): DailyBriefDao = database.dailyBriefDao()

    @Provides
    fun provideRecommendationCacheDao(database: AppDatabase): RecommendationCacheDao = database.recommendationCacheDao()

    @Provides
    fun provideAiCallLogDao(database: AppDatabase): AiCallLogDao = database.aiCallLogDao()
}
