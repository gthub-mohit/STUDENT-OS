package com.studentos.feature.settings.domain.repository

/**
 * SettingsRepository — Domain repository contract for Student OS settings.
 *
 * Provides typed accessors and mutators for all 17 configurable parameters
 * specified in the design document, backed by the Room `settings` key-value table.
 */
interface SettingsRepository {

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
    }
}
