package com.studentos.feature.settings.data.repository

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
import com.studentos.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SettingsRepositoryImpl — Data repository implementation for Student OS settings.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override suspend fun getAttendanceThreshold(): Int =
        getInt(SettingsRepository.KEY_ATTENDANCE_THRESHOLD, SettingsRepository.DEFAULT_ATTENDANCE_THRESHOLD)

    override suspend fun setAttendanceThreshold(value: Int) =
        set(SettingsRepository.KEY_ATTENDANCE_THRESHOLD, value.toString())

    override suspend fun getCpSyncIntervalMinutes(): Int =
        getInt(SettingsRepository.KEY_CP_SYNC_INTERVAL_MINUTES, SettingsRepository.DEFAULT_CP_SYNC_INTERVAL_MINUTES)

    override suspend fun setCpSyncIntervalMinutes(value: Int) =
        set(SettingsRepository.KEY_CP_SYNC_INTERVAL_MINUTES, value.toString())

    override suspend fun getDailyBriefTimeHHmm(): String =
        getString(SettingsRepository.KEY_DAILY_BRIEF_TIME, SettingsRepository.DEFAULT_DAILY_BRIEF_TIME)

    override suspend fun setDailyBriefTimeHHmm(value: String) =
        set(SettingsRepository.KEY_DAILY_BRIEF_TIME, value)

    override suspend fun getDefaultAssignmentReminderLeadMs(): Long =
        getLong(SettingsRepository.KEY_DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS, SettingsRepository.DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS)

    override suspend fun setDefaultAssignmentReminderLeadMs(value: Long) =
        set(SettingsRepository.KEY_DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS, value.toString())

    override suspend fun getContestReminderLookaheadMs(): Long =
        getLong(SettingsRepository.KEY_CONTEST_REMINDER_LOOKAHEAD_MS, SettingsRepository.DEFAULT_CONTEST_REMINDER_LOOKAHEAD_MS)

    override suspend fun setContestReminderLookaheadMs(value: Long) =
        set(SettingsRepository.KEY_CONTEST_REMINDER_LOOKAHEAD_MS, value.toString())

    override suspend fun getProjectInactivityThresholdDays(): Int =
        getInt(SettingsRepository.KEY_PROJECT_INACTIVITY_THRESHOLD_DAYS, SettingsRepository.DEFAULT_PROJECT_INACTIVITY_THRESHOLD_DAYS)

    override suspend fun setProjectInactivityThresholdDays(value: Int) =
        set(SettingsRepository.KEY_PROJECT_INACTIVITY_THRESHOLD_DAYS, value.toString())

    override suspend fun getScoreWeightClass(): Int =
        getInt(SettingsRepository.KEY_SCORE_WEIGHT_CLASS, SettingsRepository.DEFAULT_SCORE_WEIGHT_CLASS)

    override suspend fun setScoreWeightClass(value: Int) =
        set(SettingsRepository.KEY_SCORE_WEIGHT_CLASS, value.toString())

    override suspend fun getScoreWeightAssignment(): Int =
        getInt(SettingsRepository.KEY_SCORE_WEIGHT_ASSIGNMENT, SettingsRepository.DEFAULT_SCORE_WEIGHT_ASSIGNMENT)

    override suspend fun setScoreWeightAssignment(value: Int) =
        set(SettingsRepository.KEY_SCORE_WEIGHT_ASSIGNMENT, value.toString())

    override suspend fun getScoreWeightProjectAction(): Int =
        getInt(SettingsRepository.KEY_SCORE_WEIGHT_PROJECT_ACTION, SettingsRepository.DEFAULT_SCORE_WEIGHT_PROJECT_ACTION)

    override suspend fun setScoreWeightProjectAction(value: Int) =
        set(SettingsRepository.KEY_SCORE_WEIGHT_PROJECT_ACTION, value.toString())

    override suspend fun getScoreWeightDsa(): Int =
        getInt(SettingsRepository.KEY_SCORE_WEIGHT_DSA, SettingsRepository.DEFAULT_SCORE_WEIGHT_DSA)

    override suspend fun setScoreWeightDsa(value: Int) =
        set(SettingsRepository.KEY_SCORE_WEIGHT_DSA, value.toString())

    override suspend fun getOcrConfidenceThreshold(): Float =
        getFloat(SettingsRepository.KEY_OCR_CONFIDENCE_THRESHOLD, SettingsRepository.DEFAULT_OCR_CONFIDENCE_THRESHOLD)

    override suspend fun setOcrConfidenceThreshold(value: Float) =
        set(SettingsRepository.KEY_OCR_CONFIDENCE_THRESHOLD, value.toString())

    override suspend fun getAiEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_AI_ENABLED, SettingsRepository.DEFAULT_AI_ENABLED)

    override suspend fun setAiEnabled(value: Boolean) =
        set(SettingsRepository.KEY_AI_ENABLED, value.toString())

    override suspend fun getAiProvider(): String =
        getString(SettingsRepository.KEY_AI_PROVIDER, SettingsRepository.DEFAULT_AI_PROVIDER)

    override suspend fun setAiProvider(value: String) =
        set(SettingsRepository.KEY_AI_PROVIDER, value)

    override suspend fun getAiIntradayUpdatesEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_AI_INTRADAY_UPDATES_ENABLED, SettingsRepository.DEFAULT_AI_INTRADAY_UPDATES_ENABLED)

    override suspend fun setAiIntradayUpdatesEnabled(value: Boolean) =
        set(SettingsRepository.KEY_AI_INTRADAY_UPDATES_ENABLED, value.toString())

    override suspend fun getAiMaxCallsPerDay(): Int =
        getInt(SettingsRepository.KEY_AI_MAX_CALLS_PER_DAY, SettingsRepository.DEFAULT_AI_MAX_CALLS_PER_DAY)

    override suspend fun setAiMaxCallsPerDay(value: Int) =
        set(SettingsRepository.KEY_AI_MAX_CALLS_PER_DAY, value.toString())

    override suspend fun getAiCacheMaxAgeHours(): Int =
        getInt(SettingsRepository.KEY_AI_CACHE_MAX_AGE_HOURS, SettingsRepository.DEFAULT_AI_CACHE_MAX_AGE_HOURS)

    override suspend fun setAiCacheMaxAgeHours(value: Int) =
        set(SettingsRepository.KEY_AI_CACHE_MAX_AGE_HOURS, value.toString())

    override suspend fun getAiTonePreference(): String =
        getString(SettingsRepository.KEY_AI_TONE_PREFERENCE, SettingsRepository.DEFAULT_AI_TONE_PREFERENCE)

    override suspend fun setAiTonePreference(value: String) =
        set(SettingsRepository.KEY_AI_TONE_PREFERENCE, value)

    override suspend fun reset() {
        setAttendanceThreshold(SettingsRepository.DEFAULT_ATTENDANCE_THRESHOLD)
        setCpSyncIntervalMinutes(SettingsRepository.DEFAULT_CP_SYNC_INTERVAL_MINUTES)
        setDailyBriefTimeHHmm(SettingsRepository.DEFAULT_DAILY_BRIEF_TIME)
        setDefaultAssignmentReminderLeadMs(SettingsRepository.DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS)
        setContestReminderLookaheadMs(SettingsRepository.DEFAULT_CONTEST_REMINDER_LOOKAHEAD_MS)
        setProjectInactivityThresholdDays(SettingsRepository.DEFAULT_PROJECT_INACTIVITY_THRESHOLD_DAYS)
        setScoreWeightClass(SettingsRepository.DEFAULT_SCORE_WEIGHT_CLASS)
        setScoreWeightAssignment(SettingsRepository.DEFAULT_SCORE_WEIGHT_ASSIGNMENT)
        setScoreWeightProjectAction(SettingsRepository.DEFAULT_SCORE_WEIGHT_PROJECT_ACTION)
        setScoreWeightDsa(SettingsRepository.DEFAULT_SCORE_WEIGHT_DSA)
        setOcrConfidenceThreshold(SettingsRepository.DEFAULT_OCR_CONFIDENCE_THRESHOLD)
        setAiEnabled(SettingsRepository.DEFAULT_AI_ENABLED)
        setAiProvider(SettingsRepository.DEFAULT_AI_PROVIDER)
        setAiIntradayUpdatesEnabled(SettingsRepository.DEFAULT_AI_INTRADAY_UPDATES_ENABLED)
        setAiMaxCallsPerDay(SettingsRepository.DEFAULT_AI_MAX_CALLS_PER_DAY)
        setAiCacheMaxAgeHours(SettingsRepository.DEFAULT_AI_CACHE_MAX_AGE_HOURS)
        setAiTonePreference(SettingsRepository.DEFAULT_AI_TONE_PREFERENCE)
    }

    override suspend fun exportAll(): Map<String, String> =
        settingsDao.getAll().associate { it.key to it.value }

    private suspend fun getString(key: String, default: String): String =
        settingsDao.get(key) ?: default

    private suspend fun getInt(key: String, default: Int): Int =
        settingsDao.get(key)?.toIntOrNull() ?: default

    private suspend fun getLong(key: String, default: Long): Long =
        settingsDao.get(key)?.toLongOrNull() ?: default

    private suspend fun getFloat(key: String, default: Float): Float =
        settingsDao.get(key)?.toFloatOrNull() ?: default

    private suspend fun getBoolean(key: String, default: Boolean): Boolean =
        settingsDao.get(key)?.toBooleanStrictOrNull() ?: default

    private suspend fun set(key: String, value: String) {
        settingsDao.set(SettingEntity(key = key, value = value))
    }
}
