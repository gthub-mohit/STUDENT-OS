package com.studentos.feature.intelligence.domain.usecase

import com.studentos.core.database.dao.ActivityLogDao
import com.studentos.core.database.dao.CommitmentDao
import com.studentos.core.database.dao.JournalDao
import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.CommitmentEntity
import com.studentos.feature.intelligence.domain.model.EditableActivity
import com.studentos.feature.intelligence.domain.model.EditableCommitment
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class PersistenceResult(
    val persistedActivitiesCount: Int,
    val persistedCommitmentsCount: Int,
    val skippedDuplicatesCount: Int
)

class ConfirmAndPersistActivitiesUseCase @Inject constructor(
    private val activityLogDao: ActivityLogDao,
    private val commitmentDao: CommitmentDao,
    private val journalDao: JournalDao
) {

    suspend operator fun invoke(
        journalEntryId: Long,
        confirmedActivities: List<EditableActivity>,
        confirmedCommitments: List<EditableCommitment>,
        summary: String?,
        rawInput: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): PersistenceResult {
        var persistedActivities = 0
        var skippedDuplicates = 0

        // 1. Persist activities with safe deduplication checks
        confirmedActivities.forEachIndexed { index, activity ->
            val cleanTitle = activity.title.trim()
            if (cleanTitle.isNotBlank()) {
                val dedupHash = computeDedupHash(
                    entryId = journalEntryId,
                    index = index,
                    title = cleanTitle,
                    delta = activity.metricDelta,
                    duration = activity.durationMinutes,
                    goalId = activity.matchedGoalId
                )

                // Safe deduplication: check if this exact item from this confirmation batch was already inserted
                val existing = activityLogDao.findByDedupHash(dedupHash)
                if (existing != null) {
                    skippedDuplicates++
                } else {
                    activityLogDao.insert(
                        ActivityLogEntity(
                            goalId = activity.matchedGoalId,
                            commitmentId = null,
                            timestamp = timestamp,
                            metricDelta = activity.metricDelta ?: 1.0,
                            activityType = activity.activityType.ifBlank { "GENERAL" },
                            summary = cleanTitle,
                            rawInput = rawInput,
                            durationMinutes = activity.durationMinutes,
                            dedupHash = dedupHash,
                            confirmedAt = timestamp
                        )
                    )
                    persistedActivities++
                }
            }
        }

        // 2. Persist commitments
        var persistedCommitments = 0
        confirmedCommitments.forEach { commitment ->
            val cleanTitle = commitment.title.trim()
            if (cleanTitle.isNotBlank()) {
                val deadlineEpoch = parseDeadlineToEpoch(
                    targetDate = commitment.targetDate,
                    targetTime = commitment.targetTime
                )

                commitmentDao.insert(
                    CommitmentEntity(
                        title = cleanTitle,
                        category = commitment.category.ifBlank { "ACADEMIC" },
                        deadline = deadlineEpoch,
                        priority = commitment.priority.ifBlank { "MEDIUM" },
                        status = "PENDING",
                        estimatedMinutes = commitment.estimatedMinutes,
                        notes = null,
                        createdAt = timestamp
                    )
                )
                persistedCommitments++
            }
        }

        // 3. Update the journal entry with structured summary
        if (!summary.isNullOrBlank()) {
            journalDao.updateSummary(
                id = journalEntryId,
                summary = summary,
                updatedAt = timestamp
            )
        }

        return PersistenceResult(
            persistedActivitiesCount = persistedActivities,
            persistedCommitmentsCount = persistedCommitments,
            skippedDuplicatesCount = skippedDuplicates
        )
    }

    private fun computeDedupHash(
        entryId: Long,
        index: Int,
        title: String,
        delta: Double?,
        duration: Int?,
        goalId: Long?
    ): String {
        val rawKey = "$entryId:$index:${title.lowercase()}:$delta:$duration:$goalId"
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun parseDeadlineToEpoch(targetDate: String?, targetTime: String?): Long? {
        if (targetDate.isNullOrBlank()) return null
        return try {
            val date = LocalDate.parse(targetDate.trim())
            val time = if (!targetTime.isNullOrBlank()) {
                try {
                    LocalTime.parse(targetTime.trim())
                } catch (_: Exception) {
                    LocalTime.of(23, 59)
                }
            } else {
                LocalTime.of(23, 59)
            }
            val dateTime = LocalDateTime.of(date, time)
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
