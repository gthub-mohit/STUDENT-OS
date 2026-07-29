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
            .addCallback(object : RoomDatabase.Callback() {
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

    @Provides
    @Singleton
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    @Singleton
    fun provideTimetableSlotDao(database: AppDatabase): TimetableSlotDao {
        return database.timetableSlotDao()
    }

    @Provides
    @Singleton
    fun provideClassEventDao(database: AppDatabase): ClassEventDao {
        return database.classEventDao()
    }

    @Provides
    @Singleton
    fun provideAssignmentDao(database: AppDatabase): AssignmentDao {
        return database.assignmentDao()
    }

    @Provides
    @Singleton
    fun provideCpProfileDao(database: AppDatabase): CpProfileDao {
        return database.cpProfileDao()
    }

    @Provides
    @Singleton
    fun provideCpContestDao(database: AppDatabase): CpContestDao {
        return database.cpContestDao()
    }

    @Provides
    @Singleton
    fun provideCpReflectionDao(database: AppDatabase): CpReflectionDao {
        return database.cpReflectionDao()
    }

    @Provides
    @Singleton
    fun provideDsaCategoryDao(database: AppDatabase): DsaCategoryDao {
        return database.dsaCategoryDao()
    }

    @Provides
    @Singleton
    fun provideDsaTopicDao(database: AppDatabase): DsaTopicDao {
        return database.dsaTopicDao()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    @Singleton
    fun provideProjectTaskDao(database: AppDatabase): ProjectTaskDao {
        return database.projectTaskDao()
    }

    @Provides
    @Singleton
    fun provideMilestoneDao(database: AppDatabase): MilestoneDao {
        return database.milestoneDao()
    }

    @Provides
    @Singleton
    fun provideBugDao(database: AppDatabase): BugDao {
        return database.bugDao()
    }

    @Provides
    @Singleton
    fun provideProjectResourceDao(database: AppDatabase): ProjectResourceDao {
        return database.projectResourceDao()
    }

    @Provides
    @Singleton
    fun provideDailyBriefDao(database: AppDatabase): DailyBriefDao {
        return database.dailyBriefDao()
    }

    @Provides
    @Singleton
    fun provideRecommendationCacheDao(database: AppDatabase): RecommendationCacheDao {
        return database.recommendationCacheDao()
    }

    @Provides
    @Singleton
    fun provideAiCallLogDao(database: AppDatabase): AiCallLogDao {
        return database.aiCallLogDao()
    }
}
