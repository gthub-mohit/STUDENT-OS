package com.studentos.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.studentos.core.database.entity.AiCallLogEntity
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.BugEntity
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.database.entity.CpReflectionEntity
import com.studentos.core.database.entity.DailyBriefEntity
import com.studentos.core.database.entity.DsaCategoryEntity
import com.studentos.core.database.entity.DsaTopicEntity
import com.studentos.core.database.entity.MilestoneEntity
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.database.entity.ProjectResourceEntity
import com.studentos.core.database.entity.ProjectTaskEntity
import com.studentos.core.database.entity.RecommendationCacheEntity
import com.studentos.core.database.entity.SettingEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity

/**
 * AppDatabase — Main Room database class for Student OS.
 *
 * Configured with version = 1 and exportSchema = true.
 */
@Database(
    entities = [
        SettingEntity::class,
        SubjectEntity::class,
        TimetableSlotEntity::class,
        ClassEventEntity::class,
        AssignmentEntity::class,
        CpProfileEntity::class,
        CpContestEntity::class,
        CpReflectionEntity::class,
        DsaCategoryEntity::class,
        DsaTopicEntity::class,
        ProjectEntity::class,
        MilestoneEntity::class,
        BugEntity::class,
        ProjectTaskEntity::class,
        ProjectResourceEntity::class,
        DailyBriefEntity::class,
        RecommendationCacheEntity::class,
        AiCallLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableSlotDao(): TimetableSlotDao
    abstract fun classEventDao(): ClassEventDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun cpProfileDao(): CpProfileDao
    abstract fun cpContestDao(): CpContestDao
    abstract fun cpReflectionDao(): CpReflectionDao
    abstract fun dsaCategoryDao(): DsaCategoryDao
    abstract fun dsaTopicDao(): DsaTopicDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectTaskDao(): ProjectTaskDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun bugDao(): BugDao
    abstract fun projectResourceDao(): ProjectResourceDao
    abstract fun dailyBriefDao(): DailyBriefDao
    abstract fun recommendationCacheDao(): RecommendationCacheDao
    abstract fun aiCallLogDao(): AiCallLogDao

    companion object {
        const val DATABASE_NAME = "student_os.db"
    }
}
