package com.studentos.core.intelligence.snapshot

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotDiffer @Inject constructor() {

    fun diff(old: IntelligenceSnapshot?, new: IntelligenceSnapshot): SnapshotDelta {
        if (old == null) {
            return SnapshotDelta(
                dateChanged = true,
                studentContextChanged = new.studentContext.name != null,
                classesDelta = ClassesDelta(added = new.classesToday),
                attendanceDelta = AttendanceDelta(addedWarnings = new.attendanceWarnings),
                assignmentsDelta = AssignmentsDelta(added = new.assignmentsUrgent),
                freeSlotsDelta = FreeSlotsDelta(added = new.freeSlots),
                dsaTopicDelta = DsaTopicDelta(changed = new.suggestedDsaTopic != null, newTopic = new.suggestedDsaTopic),
                projectActionDelta = ProjectActionDelta(changed = new.suggestedProjectAction != null, newAction = new.suggestedProjectAction),
                scoreDelta = ScoreDelta(
                    targetChanged = new.score.target != 0,
                    newTarget = new.score.target,
                    actualChanged = new.score.actual != 0,
                    newActual = new.score.actual
                ),
                cpSummaryDelta = CpSummaryDelta(
                    ratingChanged = new.cpSummary.codechefRating != 0 || new.cpSummary.codeforcesRating != 0,
                    codechefChanged = new.cpSummary.codechefRating != 0,
                    codeforcesChanged = new.cpSummary.codeforcesRating != 0
                )
            )
        }

        val dateChanged = old.date != new.date
        val studentContextChanged = old.studentContext != new.studentContext

        // Classes diff
        val classesAdded = new.classesToday.filter { n -> old.classesToday.none { o -> o.subject == n.subject && o.time == n.time } }
        val classesRemoved = old.classesToday.filter { o -> new.classesToday.none { n -> n.subject == o.subject && n.time == o.time } }
        val classesDelta = ClassesDelta(added = classesAdded, removed = classesRemoved)

        // Attendance diff
        val oldWarnMap = old.attendanceWarnings.associateBy { it.subject }
        val newWarnMap = new.attendanceWarnings.associateBy { it.subject }

        val addedWarnings = new.attendanceWarnings.filter { !oldWarnMap.containsKey(it.subject) }
        val removedWarnings = old.attendanceWarnings.filter { !newWarnMap.containsKey(it.subject) }
        val updatedWarnings = new.attendanceWarnings.filter { n ->
            val o = oldWarnMap[n.subject]
            o != null && (o.percentage != n.percentage || o.canSkip != n.canSkip || o.mustAttend != n.mustAttend)
        }
        val attendanceDelta = AttendanceDelta(
            addedWarnings = addedWarnings,
            removedWarnings = removedWarnings,
            updatedWarnings = updatedWarnings
        )

        // Assignments diff
        val oldAssignMap = old.assignmentsUrgent.associateBy { it.id }
        val newAssignMap = new.assignmentsUrgent.associateBy { it.id }

        val addedAssignments = new.assignmentsUrgent.filter { !oldAssignMap.containsKey(it.id) }
        val removedAssignments = old.assignmentsUrgent.filter { !newAssignMap.containsKey(it.id) }
        val updatedAssignments = new.assignmentsUrgent.filter { n ->
            val o = oldAssignMap[n.id]
            o != null && (o.status != n.status || o.hoursRemaining != n.hoursRemaining || o.deadline != n.deadline)
        }
        val assignmentsDelta = AssignmentsDelta(
            added = addedAssignments,
            removed = removedAssignments,
            updated = updatedAssignments
        )

        // Free slots diff
        val freeSlotsAdded = new.freeSlots.filter { n -> old.freeSlots.none { o -> o.start == n.start && o.end == n.end } }
        val freeSlotsRemoved = old.freeSlots.filter { o -> new.freeSlots.none { n -> n.start == o.start && n.end == o.end } }
        val freeSlotsDelta = FreeSlotsDelta(added = freeSlotsAdded, removed = freeSlotsRemoved)

        // DSA topic diff
        val dsaTopicChanged = old.suggestedDsaTopic != new.suggestedDsaTopic
        val dsaTopicDelta = DsaTopicDelta(
            changed = dsaTopicChanged,
            oldTopic = old.suggestedDsaTopic,
            newTopic = new.suggestedDsaTopic
        )

        // Project action diff
        val projectActionChanged = old.suggestedProjectAction != new.suggestedProjectAction
        val projectActionDelta = ProjectActionDelta(
            changed = projectActionChanged,
            oldAction = old.suggestedProjectAction,
            newAction = new.suggestedProjectAction
        )

        // Score diff
        val targetChanged = old.score.target != new.score.target
        val actualChanged = old.score.actual != new.score.actual
        val scoreDelta = ScoreDelta(
            targetChanged = targetChanged,
            oldTarget = old.score.target,
            newTarget = new.score.target,
            actualChanged = actualChanged,
            oldActual = old.score.actual,
            newActual = new.score.actual
        )

        // CP summary diff - ignore lastSynced timestamp differences, only compare actual ratings!
        val codechefChanged = old.cpSummary.codechefRating != new.cpSummary.codechefRating
        val codeforcesChanged = old.cpSummary.codeforcesRating != new.cpSummary.codeforcesRating
        val ratingChanged = codechefChanged || codeforcesChanged
        val cpSummaryDelta = CpSummaryDelta(
            ratingChanged = ratingChanged,
            codechefChanged = codechefChanged,
            codeforcesChanged = codeforcesChanged
        )

        return SnapshotDelta(
            dateChanged = dateChanged,
            studentContextChanged = studentContextChanged,
            classesDelta = classesDelta,
            attendanceDelta = attendanceDelta,
            assignmentsDelta = assignmentsDelta,
            freeSlotsDelta = freeSlotsDelta,
            dsaTopicDelta = dsaTopicDelta,
            projectActionDelta = projectActionDelta,
            scoreDelta = scoreDelta,
            cpSummaryDelta = cpSummaryDelta
        )
    }
}
