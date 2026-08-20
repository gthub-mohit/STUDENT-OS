package com.studentos.feature.settings.domain.repository

import com.studentos.feature.settings.domain.model.SettingsDomain
import kotlinx.coroutines.flow.Flow

/**
 * SettingsRepository — Domain repository contract for Student OS settings.
 *
 * Provides typed accessors and mutators for all configurable parameters,
 * backed by the Room `settings` key-value table.
 */
interface SettingsRepository {

    fun observeAllSettings(): Flow<SettingsDomain>
    suspend fun getAllSettings(): SettingsDomain

    suspend fun getAttendanceThreshold(): Int
    suspend fun setAttendanceThreshold(value: Int)

    suspend fun getCpSyncIntervalMinutes(): Int
    suspend fun setCpSyncIntervalMinutes(value: Int)

    suspend fun getDailyBriefTimeHHmm(): String
    suspend fun setDailyBriefTimeHHmm(value: String)

    suspend fun getDefaultAssignmentReminderLeadMs(): Long
    suspend fun setDefaultAssignmentReminderLeadMs(value: Long)

    suspend fun getContestReminderLookaheadMs(): Long
    suspend fun setContestReminderLookaheadMs(value: Long)

    suspend fun getProjectInactivityThresholdDays(): Int
    suspend fun setProjectInactivityThresholdDays(value: Int)

    suspend fun getScoreWeightClass(): Int
    suspend fun setScoreWeightClass(value: Int)

    suspend fun getScoreWeightAssignment(): Int
    suspend fun setScoreWeightAssignment(value: Int)

    suspend fun getScoreWeightProjectAction(): Int
    suspend fun setScoreWeightProjectAction(value: Int)

    suspend fun getScoreWeightDsa(): Int
    suspend fun setScoreWeightDsa(value: Int)

    suspend fun getOcrConfidenceThreshold(): Float
    suspend fun setOcrConfidenceThreshold(value: Float)

    suspend fun getAiEnabled(): Boolean
    suspend fun setAiEnabled(value: Boolean)

    suspend fun getAiProvider(): String
    suspend fun setAiProvider(value: String)

    suspend fun getAiIntradayUpdatesEnabled(): Boolean
    suspend fun setAiIntradayUpdatesEnabled(value: Boolean)

    suspend fun getAiMaxCallsPerDay(): Int
    suspend fun setAiMaxCallsPerDay(value: Int)

    suspend fun getAiCacheMaxAgeHours(): Int
    suspend fun setAiCacheMaxAgeHours(value: Int)

    suspend fun getAiTonePreference(): String
    suspend fun setAiTonePreference(value: String)

    suspend fun getCodeChefHandle(): String
    suspend fun setCodeChefHandle(value: String)

    suspend fun getCodeforcesHandle(): String
    suspend fun setCodeforcesHandle(value: String)

    suspend fun getDeepSeekApiKey(): String
    suspend fun setDeepSeekApiKey(value: String)

    suspend fun getNotificationDailyBriefEnabled(): Boolean
    suspend fun setNotificationDailyBriefEnabled(value: Boolean)

    suspend fun getNotificationAssignmentReminderEnabled(): Boolean
    suspend fun setNotificationAssignmentReminderEnabled(value: Boolean)

    suspend fun getNotificationClassReminderEnabled(): Boolean
    suspend fun setNotificationClassReminderEnabled(value: Boolean)

    suspend fun getNotificationClassReminderLeadMinutes(): Int
    suspend fun setNotificationClassReminderLeadMinutes(value: Int)

    suspend fun getNotificationContestReminderEnabled(): Boolean
    suspend fun setNotificationContestReminderEnabled(value: Boolean)

    suspend fun getNotificationFreeSlotEnabled(): Boolean
    suspend fun setNotificationFreeSlotEnabled(value: Boolean)

    suspend fun getNotificationInactiveProjectEnabled(): Boolean
    suspend fun setNotificationInactiveProjectEnabled(value: Boolean)

    /**
     * Resets all settings to their default values.
     */
    suspend fun reset()

    /**
     * Exports all current key-value pairs stored in the settings table.
     */
    suspend fun exportAll(): Map<String, String>

    companion object {
        const val KEY_ATTENDANCE_THRESHOLD = "attendance_threshold"
        const val KEY_CP_SYNC_INTERVAL_MINUTES = "cp_sync_interval_minutes"
        const val KEY_DAILY_BRIEF_TIME = "daily_brief_time"
        const val KEY_DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS = "default_assignment_reminder_lead_ms"
        const val KEY_CONTEST_REMINDER_LOOKAHEAD_MS = "contest_reminder_lookahead_ms"
        const val KEY_PROJECT_INACTIVITY_THRESHOLD_DAYS = "project_inactivity_threshold_days"
        const val KEY_SCORE_WEIGHT_CLASS = "score_weight_class"
        const val KEY_SCORE_WEIGHT_ASSIGNMENT = "score_weight_assignment"
        const val KEY_SCORE_WEIGHT_PROJECT_ACTION = "score_weight_project_action"
        const val KEY_SCORE_WEIGHT_DSA = "score_weight_dsa"
        const val KEY_OCR_CONFIDENCE_THRESHOLD = "ocr_confidence_threshold"
        const val KEY_AI_ENABLED = "ai_enabled"
        const val KEY_AI_PROVIDER = "ai_provider"
        const val KEY_AI_INTRADAY_UPDATES_ENABLED = "ai_intraday_updates_enabled"
        const val KEY_AI_MAX_CALLS_PER_DAY = "ai_max_calls_per_day"
        const val KEY_AI_CACHE_MAX_AGE_HOURS = "ai_cache_max_age_hours"
        const val KEY_AI_TONE_PREFERENCE = "ai_tone_preference"
        const val KEY_HANDLE_CODECHEF = "handle_codechef"
        const val KEY_HANDLE_CODEFORCES = "handle_codeforces"
        const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        const val KEY_NOTIFICATION_DAILY_BRIEF_ENABLED = "notification_daily_brief_enabled"
        const val KEY_NOTIFICATION_ASSIGNMENT_REMINDER_ENABLED = "notification_assignment_reminder_enabled"
        const val KEY_NOTIFICATION_CLASS_REMINDER_ENABLED = "notification_class_reminder_enabled"
        const val KEY_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES = "notification_class_reminder_lead_minutes"
        const val KEY_NOTIFICATION_CONTEST_REMINDER_ENABLED = "notification_contest_reminder_enabled"
        const val KEY_NOTIFICATION_FREE_SLOT_ENABLED = "notification_free_slot_enabled"
        const val KEY_NOTIFICATION_INACTIVE_PROJECT_ENABLED = "notification_inactive_project_enabled"

        // Defaults
        const val DEFAULT_ATTENDANCE_THRESHOLD = 75
        const val DEFAULT_CP_SYNC_INTERVAL_MINUTES = 360
        const val DEFAULT_DAILY_BRIEF_TIME = "07:00"
        const val DEFAULT_ASSIGNMENT_REMINDER_LEAD_MS = 86_400_000L
        const val DEFAULT_CONTEST_REMINDER_LOOKAHEAD_MS = 86_400_000L
        const val DEFAULT_PROJECT_INACTIVITY_THRESHOLD_DAYS = 7
        const val DEFAULT_SCORE_WEIGHT_CLASS = 10
        const val DEFAULT_SCORE_WEIGHT_ASSIGNMENT = 20
        const val DEFAULT_SCORE_WEIGHT_PROJECT_ACTION = 15
        const val DEFAULT_SCORE_WEIGHT_DSA = 10
        const val DEFAULT_OCR_CONFIDENCE_THRESHOLD = 0.80f
        const val DEFAULT_AI_ENABLED = true
        const val DEFAULT_AI_PROVIDER = "DEEPSEEK"
        const val DEFAULT_AI_INTRADAY_UPDATES_ENABLED = true
        const val DEFAULT_AI_MAX_CALLS_PER_DAY = 10
        const val DEFAULT_AI_CACHE_MAX_AGE_HOURS = 6
        const val DEFAULT_AI_TONE_PREFERENCE = "motivational"
        const val DEFAULT_NOTIFICATION_CLASS_REMINDER_LEAD_MINUTES = 15
    }
}
