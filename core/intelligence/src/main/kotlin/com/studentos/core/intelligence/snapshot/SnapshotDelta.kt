package com.studentos.core.intelligence.snapshot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SnapshotDelta(
    @SerialName("date_changed") val dateChanged: Boolean = false,
    @SerialName("student_context_changed") val studentContextChanged: Boolean = false,
    @SerialName("classes_delta") val classesDelta: ClassesDelta = ClassesDelta(),
    @SerialName("attendance_delta") val attendanceDelta: AttendanceDelta = AttendanceDelta(),
    @SerialName("assignments_delta") val assignmentsDelta: AssignmentsDelta = AssignmentsDelta(),
    @SerialName("free_slots_delta") val freeSlotsDelta: FreeSlotsDelta = FreeSlotsDelta(),
    @SerialName("dsa_topic_delta") val dsaTopicDelta: DsaTopicDelta = DsaTopicDelta(),
    @SerialName("project_action_delta") val projectActionDelta: ProjectActionDelta = ProjectActionDelta(),
    @SerialName("score_delta") val scoreDelta: ScoreDelta = ScoreDelta(),
    @SerialName("cp_summary_delta") val cpSummaryDelta: CpSummaryDelta = CpSummaryDelta()
) {
    val isEmpty: Boolean
        get() = !dateChanged &&
                !studentContextChanged &&
                classesDelta.isEmpty &&
                attendanceDelta.isEmpty &&
                assignmentsDelta.isEmpty &&
                freeSlotsDelta.isEmpty &&
                dsaTopicDelta.isEmpty &&
                projectActionDelta.isEmpty &&
                scoreDelta.isEmpty &&
                cpSummaryDelta.isEmpty
}

@Serializable
data class ClassesDelta(
    @SerialName("added") val added: List<ClassTodaySnapshot> = emptyList(),
    @SerialName("removed") val removed: List<ClassTodaySnapshot> = emptyList()
) {
    val isEmpty: Boolean get() = added.isEmpty() && removed.isEmpty()
}

@Serializable
data class AttendanceDelta(
    @SerialName("added_warnings") val addedWarnings: List<AttendanceWarningSnapshot> = emptyList(),
    @SerialName("removed_warnings") val removedWarnings: List<AttendanceWarningSnapshot> = emptyList(),
    @SerialName("updated_warnings") val updatedWarnings: List<AttendanceWarningSnapshot> = emptyList()
) {
    val isEmpty: Boolean get() = addedWarnings.isEmpty() && removedWarnings.isEmpty() && updatedWarnings.isEmpty()
}

@Serializable
data class AssignmentsDelta(
    @SerialName("added") val added: List<AssignmentUrgentSnapshot> = emptyList(),
    @SerialName("removed") val removed: List<AssignmentUrgentSnapshot> = emptyList(),
    @SerialName("updated") val updated: List<AssignmentUrgentSnapshot> = emptyList()
) {
    val isEmpty: Boolean get() = added.isEmpty() && removed.isEmpty() && updated.isEmpty()
}

@Serializable
data class FreeSlotsDelta(
    @SerialName("added") val added: List<FreeSlotSnapshot> = emptyList(),
    @SerialName("removed") val removed: List<FreeSlotSnapshot> = emptyList()
) {
    val isEmpty: Boolean get() = added.isEmpty() && removed.isEmpty()
}

@Serializable
data class DsaTopicDelta(
    @SerialName("changed") val changed: Boolean = false,
    @SerialName("old_topic") val oldTopic: SuggestedDsaTopicSnapshot? = null,
    @SerialName("new_topic") val newTopic: SuggestedDsaTopicSnapshot? = null
) {
    val isEmpty: Boolean get() = !changed
}

@Serializable
data class ProjectActionDelta(
    @SerialName("changed") val changed: Boolean = false,
    @SerialName("old_action") val oldAction: SuggestedProjectActionSnapshot? = null,
    @SerialName("new_action") val newAction: SuggestedProjectActionSnapshot? = null
) {
    val isEmpty: Boolean get() = !changed
}

@Serializable
data class ScoreDelta(
    @SerialName("target_changed") val targetChanged: Boolean = false,
    @SerialName("old_target") val oldTarget: Int = 0,
    @SerialName("new_target") val newTarget: Int = 0,
    @SerialName("actual_changed") val actualChanged: Boolean = false,
    @SerialName("old_actual") val oldActual: Int = 0,
    @SerialName("new_actual") val newActual: Int = 0
) {
    val isEmpty: Boolean get() = !targetChanged && !actualChanged
}

@Serializable
data class CpSummaryDelta(
    @SerialName("rating_changed") val ratingChanged: Boolean = false,
    @SerialName("codechef_changed") val codechefChanged: Boolean = false,
    @SerialName("codeforces_changed") val codeforcesChanged: Boolean = false
) {
    val isEmpty: Boolean get() = !ratingChanged
}
