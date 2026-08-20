package com.studentos.feature.settings.data.repository

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
import com.studentos.feature.settings.domain.model.SettingsDomain
import com.studentos.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SettingsRepositoryImpl — Data repository implementation for Student OS settings.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override fun observeAllSettings(): Flow<SettingsDomain> {
        return settingsDao.observeAll().map { list ->
            val map = list.associate { it.key to it.value }
            mapToDomain(map)
        }
    }

    override suspend fun getAllSettings(): SettingsDomain {
        val map = settingsDao.getAll().associate { it.key to it.value }
        return mapToDomain(map)
    }

    private fun mapToDomain(map: Map<String, String>): SettingsDomain {
        return SettingsDomain(
            attendanceThreshold = map[SettingsRepository.KEY_ATTENDANCE_THRESHOLD]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_ATTENDANCE_THRESHOLD,
            cpSyncIntervalMinutes = map[SettingsRepository.KEY_CP_SYNC_INTERVAL_MINUTES]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_CP_SYNC_INTERVAL_MINUTES,
            dailyBriefTimeHHmm = map[SettingsRepository.KEY_DAILY_BRIEF_TIME]
                ?: SettingsRepository.DEFAULT_DAILY_BRIEF_TIME,
            defaultAssignmentReminderLeadMs = map[SettingsRepository.KEY_DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS]?.toLongOrNull()
                ?: SettingsRepository.DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS,
            contestReminderLookaheadMs = map[SettingsRepository.KEY_CONTEST_REMINDER_LOOKAHEAD_MS]?.toLongOrNull()
                ?: SettingsRepository.DEFAULT_CONTEST_REMINDER_LOOKAHEAD_MS,
            projectInactivityThresholdDays = map[SettingsRepository.KEY_PROJECT_INACTIVITY_THRESHOLD_DAYS]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_PROJECT_INACTIVITY_THRESHOLD_DAYS,
            scoreWeightClass = map[SettingsRepository.KEY_SCORE_WEIGHT_CLASS]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_SCORE_WEIGHT_CLASS,
            scoreWeightAssignment = map[SettingsRepository.KEY_SCORE_WEIGHT_ASSIGNMENT]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_SCORE_WEIGHT_ASSIGNMENT,
            scoreWeightProjectAction = map[SettingsRepository.KEY_SCORE_WEIGHT_PROJECT_ACTION]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_SCORE_WEIGHT_PROJECT_ACTION,
            scoreWeightDsa = map[SettingsRepository.KEY_SCORE_WEIGHT_DSA]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_SCORE_WEIGHT_DSA,
            ocrConfidenceThreshold = map[SettingsRepository.KEY_OCR_CONFIDENCE_THRESHOLD]?.toFloatOrNull()
                ?: SettingsRepository.DEFAULT_OCR_CONFIDENCE_THRESHOLD,
            aiEnabled = map[SettingsRepository.KEY_AI_ENABLED]?.toBooleanStrictOrNull()
                ?: SettingsRepository.DEFAULT_AI_ENABLED,
            aiProvider = map[SettingsRepository.KEY_AI_PROVIDER]
                ?: SettingsRepository.DEFAULT_AI_PROVIDER,
            aiIntradayUpdatesEnabled = map[SettingsRepository.KEY_AI_INTRADAY_UPDATES_ENABLED]?.toBooleanStrictOrNull()
                ?: SettingsRepository.DEFAULT_AI_INTRADAY_UPDATES_ENABLED,
            aiMaxCallsPerDay = map[SettingsRepository.KEY_AI_MAX_CALLS_PER_DAY]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_AI_MAX_CALLS_PER_DAY,
            aiCacheMaxAgeHours = map[SettingsRepository.KEY_AI_CACHE_MAX_AGE_HOURS]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_AI_CACHE_MAX_AGE_HOURS,
            aiTonePreference = map[SettingsRepository.KEY_AI_TONE_PREFERENCE]
                ?: SettingsRepository.DEFAULT_AI_TONE_PREFERENCE,
            codeChefHandle = map[SettingsRepository.KEY_HANDLE_CODECHEF] ?: "",
            codeforcesHandle = map[SettingsRepository.KEY_HANDLE_CODEFORCES] ?: "",
            deepSeekApiKey = map[SettingsRepository.KEY_DEEPSEEK_API_KEY] ?: "",
            notificationDailyBriefEnabled = map[SettingsRepository.KEY_NOTIFICATION_DAILY_BRIEF_ENABLED]?.toBooleanStrictOrNull() ?: true,
            notificationAssignmentReminderEnabled = map[SettingsRepository.KEY_NOTIFICATION_ASSIGNMENT_REMINDER_ENABLED]?.toBooleanStrictOrNull() ?: true,
            notificationClassReminderEnabled = map[SettingsRepository.KEY_NOTIFICATION_CLASS_REMINDER_ENABLED]?.toBooleanStrictOrNull() ?: true,
            notificationClassReminderLeadMinutes = map[SettingsRepository.KEY_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES]?.toIntOrNull()
                ?: SettingsRepository.DEFAULT_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES,
            notificationContestReminderEnabled = map[SettingsRepository.KEY_NOTIFICATION_CONTEST_REMINDER_ENABLED]?.toBooleanStrictOrNull() ?: true,
            notificationFreeSlotEnabled = map[SettingsRepository.KEY_NOTIFICATION_FREE_SLOT_ENABLED]?.toBooleanStrictOrNull() ?: true,
            notificationInactiveProjectEnabled = map[SettingsRepository.KEY_NOTIFICATION_INACTIVE_PROJECT_ENABLED]?.toBooleanStrictOrNull() ?: true
        )
    }

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

    override suspend fun getCodeChefHandle(): String =
        getString(SettingsRepository.KEY_HANDLE_CODECHEF, "")

    override suspend fun setCodeChefHandle(value: String) =
        set(SettingsRepository.KEY_HANDLE_CODECHEF, value)

    override suspend fun getCodeforcesHandle(): String =
        getString(SettingsRepository.KEY_HANDLE_CODEFORCES, "")

    override suspend fun setCodeforcesHandle(value: String) =
        set(SettingsRepository.KEY_HANDLE_CODEFORCES, value)

    override suspend fun getDeepSeekApiKey(): String =
        getString(SettingsRepository.KEY_DEEPSEEK_API_KEY, "")

    override suspend fun setDeepSeekApiKey(value: String) =
        set(SettingsRepository.KEY_DEEPSEEK_API_KEY, value)

    override suspend fun getNotificationDailyBriefEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_NOTIFICATION_DAILY_BRIEF_ENABLED, true)

    override suspend fun setNotificationDailyBriefEnabled(value: Boolean) =
        set(SettingsRepository.KEY_NOTIFICATION_DAILY_BRIEF_ENABLED, value.toString())

    override suspend fun getNotificationAssignmentReminderEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_NOTIFICATION_ASSIGNMENT_REMINDER_ENABLED, true)

    override suspend fun setNotificationAssignmentReminderEnabled(value: Boolean) =
        set(SettingsRepository.KEY_NOTIFICATION_ASSIGNMENT_REMINDER_ENABLED, value.toString())

    override suspend fun getNotificationClassReminderEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_NOTIFICATION_CLASS_REMINDER_ENABLED, true)

    override suspend fun setNotificationClassReminderEnabled(value: Boolean) =
        set(SettingsRepository.KEY_NOTIFICATION_CLASS_REMINDER_ENABLED, value.toString())

    override suspend fun getNotificationClassReminderLeadMinutes(): Int =
        getInt(SettingsRepository.KEY_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES, SettingsRepository.DEFAULT_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES)

    override suspend fun setNotificationClassReminderLeadMinutes(value: Int) =
        set(SettingsRepository.KEY_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES, value.toString())

    override suspend fun getNotificationContestReminderEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_NOTIFICATION_CONTEST_REMINDER_ENABLED, true)

    override suspend fun setNotificationContestReminderEnabled(value: Boolean) =
        set(SettingsRepository.KEY_NOTIFICATION_CONTEST_REMINDER_ENABLED, value.toString())

    override suspend fun getNotificationFreeSlotEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_NOTIFICATION_FREE_SLOT_ENABLED, true)

    override suspend fun setNotificationFreeSlotEnabled(value: Boolean) =
        set(SettingsRepository.KEY_NOTIFICATION_FREE_SLOT_ENABLED, value.toString())

    override suspend fun getNotificationInactiveProjectEnabled(): Boolean =
        getBoolean(SettingsRepository.KEY_NOTIFICATION_INACTIVE_PROJECT_ENABLED, true)

    override suspend fun setNotificationInactiveProjectEnabled(value: Boolean) =
        set(SettingsRepository.KEY_NOTIFICATION_INACTIVE_PROJECT_ENABLED, value.toString())

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
        setCodeChefHandle("")
        setCodeforcesHandle("")
        setNotificationDailyBriefEnabled(true)
        setNotificationAssignmentReminderEnabled(true)
        setNotificationClassReminderEnabled(true)
        setNotificationClassReminderLeadMinutes(SettingsRepository.DEFAULT_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES)
        setNotificationContestReminderEnabled(true)
        setNotificationFreeSlotEnabled(true)
        setNotificationInactiveProjectEnabled(true)
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
