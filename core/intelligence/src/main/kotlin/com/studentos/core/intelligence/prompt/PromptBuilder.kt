package com.studentos.core.intelligence.prompt

import com.studentos.core.intelligence.snapshot.IntelligenceSnapshot
import com.studentos.core.intelligence.snapshot.SnapshotDelta
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptBuilder @Inject constructor() {

    fun buildMorningPrompt(snapshot: IntelligenceSnapshot): String {
        val sb = StringBuilder()

        sb.append("DATE=").append(snapshot.date).append("\n")

        val name = snapshot.studentContext.name
        val tone = snapshot.studentContext.tonePreference
        if (!name.isNullOrEmpty() || tone.isNotEmpty()) {
            sb.append("CONTEXT: Name=").append(name ?: "Student")
                .append(", Tone=").append(tone).append("\n")
        }

        if (snapshot.classesToday.isNotEmpty()) {
            sb.append("\nCLASSES:\n")
            for (cls in snapshot.classesToday) {
                sb.append("- ").append(cls.time).append(" ").append(cls.subject)
                if (!cls.location.isNullOrEmpty()) {
                    sb.append(" @ ").append(cls.location)
                }
                sb.append("\n")
            }
        }

        if (snapshot.attendanceWarnings.isNotEmpty()) {
            sb.append("\nATT_WARN:\n")
            for (warn in snapshot.attendanceWarnings) {
                sb.append("- ").append(warn.subject).append(" ")
                    .append(warn.percentage).append("%")
                if (warn.mustAttend > 0) {
                    sb.append(" (Must attend: ").append(warn.mustAttend).append(")")
                } else if (warn.canSkip > 0) {
                    sb.append(" (Can skip: ").append(warn.canSkip).append(")")
                }
                sb.append("\n")
            }
        }

        if (snapshot.assignmentsUrgent.isNotEmpty()) {
            sb.append("\nURGENT_ASSIGNMENTS:\n")
            for (assign in snapshot.assignmentsUrgent) {
                sb.append("- [").append(assign.id).append("] ").append(assign.subject)
                    .append(": ").append(assign.title)
                    .append(" (Status: ").append(assign.status)
                    .append(", Due: ").append(assign.hoursRemaining).append("h)\n")
            }
        }

        if (snapshot.freeSlots.isNotEmpty()) {
            sb.append("\nFREE_SLOTS:\n")
            for (slot in snapshot.freeSlots) {
                sb.append("- ").append(slot.start).append("-").append(slot.end)
                    .append(" (").append(slot.durationMinutes).append("m)\n")
            }
        }

        snapshot.suggestedDsaTopic?.let { dsa ->
            sb.append("\nDSA:\n")
            sb.append("- ").append(dsa.category).append(" -> ").append(dsa.topic)
                .append(" (Conf: ").append(dsa.confidence)
                .append(", Status: ").append(dsa.revisionStatus).append(")\n")
        }

        snapshot.suggestedProjectAction?.let { proj ->
            sb.append("\nPROJECT:\n")
            sb.append("- ").append(proj.project).append(" -> ").append(proj.action).append("\n")
        }

        if (snapshot.cpSummary.codechefRating > 0 || snapshot.cpSummary.codeforcesRating > 0) {
            sb.append("\nCP:\n")
            val parts = mutableListOf<String>()
            if (snapshot.cpSummary.codechefRating > 0) parts.add("CC=${snapshot.cpSummary.codechefRating}")
            if (snapshot.cpSummary.codeforcesRating > 0) parts.add("CF=${snapshot.cpSummary.codeforcesRating}")
            sb.append("- ").append(parts.joinToString(", ")).append("\n")
        }

        sb.append("\nSCORE:\n")
        sb.append("Target=").append(snapshot.score.target)
            .append(", Actual=").append(snapshot.score.actual)

        return sb.toString().trim()
    }

    fun buildDeltaPrompt(
        previous: IntelligenceSnapshot?,
        current: IntelligenceSnapshot,
        delta: SnapshotDelta
    ): String {
        if (delta.isEmpty) {
            return "DELTA: NO_CHANGES"
        }

        val sb = StringBuilder()
        sb.append("DELTA [").append(current.date).append("]\n")

        if (delta.dateChanged) {
            sb.append("DATE_CHANGED: ").append(previous?.date ?: "None").append(" -> ").append(current.date).append("\n")
        }

        if (delta.studentContextChanged) {
            sb.append("CONTEXT_CHANGED: ").append(current.studentContext.name ?: "Student")
                .append(" (Tone: ").append(current.studentContext.tonePreference).append(")\n")
        }

        if (!delta.classesDelta.isEmpty) {
            sb.append("\nCLASSES_CHANGED:\n")
            for (cls in delta.classesDelta.added) {
                sb.append("+ Added: ").append(cls.time).append(" ").append(cls.subject).append("\n")
            }
            for (cls in delta.classesDelta.removed) {
                sb.append("- Removed: ").append(cls.time).append(" ").append(cls.subject).append("\n")
            }
        }

        if (!delta.attendanceDelta.isEmpty) {
            sb.append("\nATTENDANCE_CHANGED:\n")
            for (warn in delta.attendanceDelta.addedWarnings) {
                sb.append("+ Warning: ").append(warn.subject).append(" ").append(warn.percentage).append("%\n")
            }
            for (warn in delta.attendanceDelta.removedWarnings) {
                sb.append("- Resolved: ").append(warn.subject).append("\n")
            }
            for (warn in delta.attendanceDelta.updatedWarnings) {
                sb.append("~ Updated: ").append(warn.subject).append(" ").append(warn.percentage).append("%\n")
            }
        }

        if (!delta.assignmentsDelta.isEmpty) {
            sb.append("\nASSIGNMENTS_CHANGED:\n")
            for (assign in delta.assignmentsDelta.added) {
                sb.append("+ Added: ").append(assign.subject).append(": ").append(assign.title)
                    .append(" (Due: ").append(assign.hoursRemaining).append("h)\n")
            }
            for (assign in delta.assignmentsDelta.removed) {
                sb.append("- Done/Removed: [").append(assign.id).append("] ").append(assign.title).append("\n")
            }
            for (assign in delta.assignmentsDelta.updated) {
                sb.append("~ Updated: [").append(assign.id).append("] ").append(assign.title)
                    .append(" (Status: ").append(assign.status)
                    .append(", Due: ").append(assign.hoursRemaining).append("h)\n")
            }
        }

        if (!delta.freeSlotsDelta.isEmpty) {
            sb.append("\nFREE_SLOTS_CHANGED:\n")
            for (slot in delta.freeSlotsDelta.added) {
                sb.append("+ Slot: ").append(slot.start).append("-").append(slot.end).append("\n")
            }
            for (slot in delta.freeSlotsDelta.removed) {
                sb.append("- Slot: ").append(slot.start).append("-").append(slot.end).append("\n")
            }
        }

        if (delta.dsaTopicDelta.changed) {
            sb.append("\nDSA_CHANGED:\n")
            val newTopic = delta.dsaTopicDelta.newTopic
            if (newTopic != null) {
                sb.append("New Topic: ").append(newTopic.category).append(" -> ").append(newTopic.topic).append("\n")
            } else {
                sb.append("DSA Mastered / Cleared\n")
            }
        }

        if (delta.projectActionDelta.changed) {
            sb.append("\nPROJECT_ACTION_CHANGED:\n")
            val newAction = delta.projectActionDelta.newAction
            if (newAction != null) {
                sb.append("New Action: ").append(newAction.project).append(" -> ").append(newAction.action).append("\n")
            } else {
                sb.append("No Next Action\n")
            }
        }

        if (!delta.cpSummaryDelta.isEmpty) {
            sb.append("\nCP_CHANGED:\n")
            if (delta.cpSummaryDelta.codechefChanged) {
                sb.append("CC=").append(current.cpSummary.codechefRating).append("\n")
            }
            if (delta.cpSummaryDelta.codeforcesChanged) {
                sb.append("CF=").append(current.cpSummary.codeforcesRating).append("\n")
            }
        }

        if (!delta.scoreDelta.isEmpty) {
            sb.append("\nSCORE_CHANGED:\n")
            if (delta.scoreDelta.targetChanged) {
                sb.append("Target: ").append(delta.scoreDelta.oldTarget).append(" -> ").append(delta.scoreDelta.newTarget).append("\n")
            }
            if (delta.scoreDelta.actualChanged) {
                sb.append("Actual: ").append(delta.scoreDelta.oldActual).append(" -> ").append(delta.scoreDelta.newActual).append("\n")
            }
        }

        return sb.toString().trim()
    }
}
