package com.studentos.feature.settings.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudentOsBackup(
    @SerialName("version") val version: Int = 1,
    @SerialName("app_version") val appVersion: String = "1.0",
    @SerialName("exported_at") val exportedAt: Long = System.currentTimeMillis(),
    @SerialName("settings") val settings: List<BackupSetting> = emptyList(),
    @SerialName("subjects") val subjects: List<BackupSubject> = emptyList(),
    @SerialName("timetable_slots") val timetableSlots: List<BackupTimetableSlot> = emptyList(),
    @SerialName("class_events") val classEvents: List<BackupClassEvent> = emptyList(),
    @SerialName("assignments") val assignments: List<BackupAssignment> = emptyList(),
    @SerialName("cp_profiles") val cpProfiles: List<BackupCpProfile> = emptyList(),
    @SerialName("cp_contests") val cpContests: List<BackupCpContest> = emptyList(),
    @SerialName("cp_reflections") val cpReflections: List<BackupCpReflection> = emptyList(),
    @SerialName("dsa_categories") val dsaCategories: List<BackupDsaCategory> = emptyList(),
    @SerialName("dsa_topics") val dsaTopics: List<BackupDsaTopic> = emptyList(),
    @SerialName("projects") val projects: List<BackupProject> = emptyList(),
    @SerialName("milestones") val milestones: List<BackupMilestone> = emptyList(),
    @SerialName("bugs") val bugs: List<BackupBug> = emptyList(),
    @SerialName("project_tasks") val projectTasks: List<BackupProjectTask> = emptyList(),
    @SerialName("project_resources") val projectResources: List<BackupProjectResource> = emptyList(),
    @SerialName("daily_briefs") val dailyBriefs: List<BackupDailyBrief> = emptyList()
)

@Serializable
data class BackupSetting(
    val key: String,
    val value: String
)

@Serializable
data class BackupSubject(
    val id: Long,
    val name: String,
    @SerialName("archived_at") val archivedAt: Long? = null
)

@Serializable
data class BackupTimetableSlot(
    val id: Long,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val location: String? = null,
    @SerialName("week_parity") val weekParity: String? = null,
    @SerialName("valid_from") val validFrom: Long,
    @SerialName("valid_until") val validUntil: Long? = null
)

@Serializable
data class BackupClassEvent(
    val id: Long,
    @SerialName("timetable_slot_id") val timetableSlotId: Long? = null,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("scheduled_at") val scheduledAt: Long,
    val status: String,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("linked_slot_id") val linkedSlotId: Long? = null
)

@Serializable
data class BackupAssignment(
    val id: Long,
    @SerialName("subject_id") val subjectId: Long,
    val title: String,
    val notes: String? = null,
    val deadline: Long,
    val priority: String,
    val status: String,
    @SerialName("task_type") val taskType: String = "ASSIGNMENT",
    @SerialName("reminder_lead_ms") val reminderLeadMs: Long? = null,
    @SerialName("attachment_uri") val attachmentUri: String? = null,
    @SerialName("updated_at") val updatedAt: Long = 0
)

@Serializable
data class BackupCpProfile(
    val id: Long,
    val platform: String,
    val handle: String,
    @SerialName("current_rating") val currentRating: Int? = null,
    @SerialName("highest_rating") val highestRating: Int? = null,
    val rank: String? = null,
    @SerialName("problems_solved") val problemsSolved: Int? = null,
    @SerialName("contest_count") val contestCount: Int? = null,
    @SerialName("last_synced_at") val lastSyncedAt: Long? = null
)

@Serializable
data class BackupCpContest(
    val id: Long,
    @SerialName("profile_id") val profileId: Long,
    @SerialName("contest_name") val contestName: String,
    @SerialName("contest_date") val contestDate: Long,
    val rank: Int? = null,
    @SerialName("rating_change") val ratingChange: Int? = null,
    @SerialName("new_rating") val newRating: Int? = null,
    @SerialName("problems_solved") val problemsSolved: Int? = null
)

@Serializable
data class BackupCpReflection(
    val id: Long,
    @SerialName("contest_id") val contestId: Long,
    @SerialName("went_wrong") val wentWrong: String? = null,
    @SerialName("to_revise") val toRevise: String? = null,
    @SerialName("self_rating") val selfRating: Int
)

@Serializable
data class BackupDsaCategory(
    val id: Long,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class BackupDsaTopic(
    val id: Long,
    @SerialName("category_id") val categoryId: Long,
    val name: String,
    val difficulty: String = "MEDIUM",
    @SerialName("confidence_level") val confidenceLevel: Int = 1,
    @SerialName("revision_status") val revisionStatus: String = "NOT_STARTED",
    @SerialName("next_revision_date") val nextRevisionDate: Long? = null,
    val notes: String? = null,
    @SerialName("updated_at") val updatedAt: Long = 0
)

@Serializable
data class BackupProject(
    val id: Long,
    val title: String,
    @SerialName("archived_at") val archivedAt: Long? = null,
    @SerialName("inactivity_threshold_days") val inactivityThresholdDays: Int = 7,
    @SerialName("last_activity_at") val lastActivityAt: Long
)

@Serializable
data class BackupMilestone(
    val id: Long,
    @SerialName("project_id") val projectId: Long,
    @SerialName("target_date") val targetDate: Long? = null,
    val title: String,
    val description: String? = null,
    val status: String = "PENDING"
)

@Serializable
data class BackupBug(
    val id: Long,
    @SerialName("project_id") val projectId: Long,
    val description: String,
    val severity: String = "MEDIUM",
    val status: String = "OPEN"
)

@Serializable
data class BackupProjectTask(
    val id: Long,
    @SerialName("project_id") val projectId: Long,
    val title: String,
    @SerialName("is_next_action") val isNextAction: Boolean = false,
    @SerialName("is_parallel") val isParallel: Boolean = false,
    @SerialName("completed_at") val completedAt: Long? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("dependency_task_id") val dependencyTaskId: Long? = null,
    val priority: String = "MEDIUM",
    val deadline: Long? = null
)

@Serializable
data class BackupProjectResource(
    val id: Long,
    @SerialName("project_id") val projectId: Long,
    val title: String,
    val url: String,
    val type: String
)

@Serializable
data class BackupDailyBrief(
    val id: Long,
    val date: String,
    @SerialName("snapshot_hash") val snapshotHash: String,
    @SerialName("score_target") val scoreTarget: Int,
    @SerialName("score_actual") val scoreActual: Int = 0,
    @SerialName("llm_guidance") val llmGuidance: String? = null,
    @SerialName("guidance_source") val guidanceSource: String = "DETERMINISTIC",
    @SerialName("guidance_updated_at") val guidanceUpdatedAt: Long = 0,
    @SerialName("generated_at") val generatedAt: Long
)
