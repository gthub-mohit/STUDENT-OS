package com.studentos.feature.intelligence.domain.usecase

import com.studentos.core.database.dao.CommitmentDao
import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.dao.JournalDao
import com.studentos.core.database.entity.JournalEntryEntity
import com.studentos.core.intelligence.model.FailureReason
import com.studentos.core.intelligence.model.LLMResult
import com.studentos.core.intelligence.provider.OmniRouteProvider
import com.studentos.feature.intelligence.domain.model.ExtractionResult
import com.studentos.feature.intelligence.domain.parser.JournalExtractionParser
import java.time.LocalDate
import javax.inject.Inject

class ExtractJournalUseCase @Inject constructor(
    private val omniRouteProvider: OmniRouteProvider,
    private val journalDao: JournalDao,
    private val goalDao: GoalDao,
    private val commitmentDao: CommitmentDao
) {

    suspend operator fun invoke(rawInput: String, dateOverride: String? = null): ExtractionResult {
        val trimmedInput = rawInput.trim()
        if (trimmedInput.isBlank()) {
            return ExtractionResult.Error("Journal entry is empty.")
        }

        val todayDate = dateOverride ?: LocalDate.now().toString()
        val now = System.currentTimeMillis()

        // 1. Immediately persist raw journal note to Room so it survives restarts/failures
        val journalEntryId = try {
            journalDao.insert(
                JournalEntryEntity(
                    date = todayDate,
                    rawLog = trimmedInput,
                    structuredSummary = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } catch (e: Exception) {
            return ExtractionResult.Error(
                message = "Failed to save journal note locally: ${e.message}",
                cause = e
            )
        }

        // 2. Fetch context from Room database
        val activeGoals = try {
            goalDao.getActiveGoals()
        } catch (_: Exception) {
            emptyList()
        }

        val pendingCommitments = try {
            commitmentDao.getPendingCommitments()
        } catch (_: Exception) {
            emptyList()
        }

        // 3. Build system instruction and context-rich prompt
        val systemInstruction = buildSystemInstruction()
        val prompt = buildPrompt(
            date = todayDate,
            rawLog = trimmedInput,
            goals = activeGoals.map { "[ID: ${it.id}] ${it.title} (${it.category}, ${it.metricType}: target ${it.targetValue} ${it.unit})" },
            commitments = pendingCommitments.take(5).map { "- ${it.title} (${it.category}, Priority: ${it.priority})" }
        )

        // 4. Send request to OmniRoute
        val llmResult = try {
            omniRouteProvider.generateCompletion(
                prompt = prompt,
                systemInstruction = systemInstruction
            )
        } catch (e: Exception) {
            return ExtractionResult.Error(
                message = "Network error connecting to OmniRoute: ${e.message}. Note saved locally.",
                journalEntryId = journalEntryId,
                isOffline = true,
                cause = e
            )
        }

        return when (llmResult) {
            is LLMResult.Success -> {
                val parseResult = JournalExtractionParser.parse(llmResult.text)
                if (parseResult.isSuccess) {
                    ExtractionResult.Success(
                        response = parseResult.getOrThrow(),
                        journalEntryId = journalEntryId
                    )
                } else {
                    ExtractionResult.Error(
                        message = "Could not parse AI response: ${parseResult.exceptionOrNull()?.message ?: "Invalid JSON"}. Note saved locally.",
                        journalEntryId = journalEntryId
                    )
                }
            }
            is LLMResult.Failure -> {
                val isOffline = llmResult.reason == FailureReason.OFFLINE || llmResult.reason == FailureReason.TIMEOUT
                ExtractionResult.Error(
                    message = "AI extraction unavailable (${llmResult.reason}: ${llmResult.message}). Note saved locally.",
                    journalEntryId = journalEntryId,
                    isOffline = isOffline
                )
            }
        }
    }

    private fun buildSystemInstruction(): String {
        return """
            You are the Student OS Structured Extraction Assistant.
            Your role is to extract accomplished activities and future commitments from daily student logs.

            STRICT ACCURACY RULES:
            1. Return ONLY a valid JSON object matching the requested schema. No markdown code blocks (no ```json), no explanatory commentary.
            2. NEVER invent counts, durations, deadlines, priorities, or goal associations.
            3. If duration is not explicitly stated in the input text, leave "durationMinutes" as null. Do not guess 30, 45, or 60.
            4. If count is not stated in the input text, leave "metricDelta" as null, or 1.0 if it is a single completed unit.
            5. If a deadline date or time is not mentioned, leave "targetDate" and "targetTime" as null.
            6. For "matchedGoalId", ONLY use an ID from the provided ACTIVE GOALS list if the log unambiguously links to it. Otherwise, use null.
            7. Categorize each activity into: "DSA", "PROJECT", "ASSIGNMENT", "CLUB", "LEARNING", "GENERAL".
            8. Categorize each commitment into: "ACADEMIC", "PROJECT", "CLUB", "LEARNING", "PERSONAL".
            9. If anything in the input is ambiguous or requires user decision, add clarifying questions in "clarifications".

            JSON SCHEMA:
            {
              "summary": "Brief 1-sentence recap of what was logged",
              "activities": [
                {
                  "title": "Accomplished task or session description",
                  "activityType": "DSA",
                  "metricDelta": 3.0,
                  "durationMinutes": 45,
                  "matchedGoalId": 1,
                  "confidence": 0.95
                }
              ],
              "commitments": [
                {
                  "title": "Pending task or commitment",
                  "category": "ACADEMIC",
                  "targetDate": "YYYY-MM-DD",
                  "targetTime": "HH:mm",
                  "priority": "HIGH",
                  "estimatedMinutes": 30
                }
              ],
              "clarifications": [
                "Optional question if something was vague"
              ]
            }
        """.trimIndent()
    }

    private fun buildPrompt(
        date: String,
        rawLog: String,
        goals: List<String>,
        commitments: List<String>
    ): String {
        val goalsSection = if (goals.isNotEmpty()) {
            goals.joinToString("\n")
        } else {
            "None currently tracked in database."
        }

        val commitmentsSection = if (commitments.isNotEmpty()) {
            commitments.joinToString("\n")
        } else {
            "No pending commitments in database."
        }

        return """
            Date of Log: $date

            ACTIVE GOALS IN DATABASE:
            $goalsSection

            PENDING COMMITMENTS IN DATABASE:
            $commitmentsSection

            STUDENT DAILY LOG:
            $rawLog
        """.trimIndent()
    }
}
