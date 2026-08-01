package com.studentos.core.intelligence.snapshot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntelligenceSnapshot(
    @SerialName("snapshot_version") val snapshotVersion: Int = 1,
    @SerialName("date") val date: String,
    @SerialName("student_context") val studentContext: StudentContextSnapshot,
    @SerialName("classes_today") val classesToday: List<ClassTodaySnapshot>,
    @SerialName("attendance_warnings") val attendanceWarnings: List<AttendanceWarningSnapshot>,
    @SerialName("assignments_urgent") val assignmentsUrgent: List<AssignmentUrgentSnapshot>,
    @SerialName("free_slots") val freeSlots: List<FreeSlotSnapshot>,
    @SerialName("suggested_dsa_topic") val suggestedDsaTopic: SuggestedDsaTopicSnapshot? = null,
    @SerialName("suggested_project_action") val suggestedProjectAction: SuggestedProjectActionSnapshot? = null,
    @SerialName("score") val score: ScoreSnapshot,
    @SerialName("cp_summary") val cpSummary: CpSummarySnapshot
)

@Serializable
data class StudentContextSnapshot(
    @SerialName("name") val name: String? = null,
    @SerialName("tone_preference") val tonePreference: String = "motivational"
)

@Serializable
data class ClassTodaySnapshot(
    @SerialName("subject") val subject: String,
    @SerialName("time") val time: String,
    @SerialName("location") val location: String? = null
)

@Serializable
data class AttendanceWarningSnapshot(
    @SerialName("subject") val subject: String,
    @SerialName("percentage") val percentage: Double,
    @SerialName("threshold") val threshold: Double = 75.0,
    @SerialName("can_skip") val canSkip: Int = 0,
    @SerialName("must_attend") val mustAttend: Int = 0
)

@Serializable
data class AssignmentUrgentSnapshot(
    @SerialName("id") val id: Long,
    @SerialName("subject") val subject: String,
    @SerialName("title") val title: String,
    @SerialName("deadline") val deadline: String,
    @SerialName("status") val status: String,
    @SerialName("hours_remaining") val hoursRemaining: Long
)

@Serializable
data class FreeSlotSnapshot(
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("duration_minutes") val durationMinutes: Int
)

@Serializable
data class SuggestedDsaTopicSnapshot(
    @SerialName("category") val category: String,
    @SerialName("topic") val topic: String,
    @SerialName("confidence") val confidence: Int,
    @SerialName("revision_status") val revisionStatus: String
)

@Serializable
data class SuggestedProjectActionSnapshot(
    @SerialName("project") val project: String,
    @SerialName("action") val action: String
)

@Serializable
data class ScoreSnapshot(
    @SerialName("target") val target: Int,
    @SerialName("actual") val actual: Int = 0,
    @SerialName("weights") val weights: ScoreWeightsSnapshot = ScoreWeightsSnapshot()
)

@Serializable
data class ScoreWeightsSnapshot(
    @SerialName("class") val classWeight: Int = 10,
    @SerialName("assignment") val assignmentWeight: Int = 20,
    @SerialName("project_action") val projectActionWeight: Int = 15,
    @SerialName("dsa") val dsaWeight: Int = 10
)

@Serializable
data class CpSummarySnapshot(
    @SerialName("codechef_rating") val codechefRating: Int = 0,
    @SerialName("codeforces_rating") val codeforcesRating: Int = 0,
    @SerialName("last_synced") val lastSynced: String? = null
)
