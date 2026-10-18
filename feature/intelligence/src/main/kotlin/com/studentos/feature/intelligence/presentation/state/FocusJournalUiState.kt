package com.studentos.feature.intelligence.presentation.state

import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.CommitmentEntity
import com.studentos.core.database.entity.GoalEntity
import com.studentos.core.database.entity.JournalEntryEntity
import com.studentos.feature.intelligence.domain.model.EditableActivity
import com.studentos.feature.intelligence.domain.model.EditableCommitment

data class ExtractionPreview(
    val journalEntryId: Long,
    val summary: String,
    val activities: List<EditableActivity>,
    val commitments: List<EditableCommitment>,
    val clarifications: List<String> = emptyList(),
    val originalInput: String = ""
)

data class FocusJournalUiState(
    val rawInput: String = "",
    val isExtracting: Boolean = false,
    val preview: ExtractionPreview? = null,
    val todayActivities: List<ActivityLogEntity> = emptyList(),
    val pendingCommitments: List<CommitmentEntity> = emptyList(),
    val recentJournalEntries: List<JournalEntryEntity> = emptyList(),
    val activeGoals: List<GoalEntity> = emptyList(),
    val userMessage: String? = null,
    val errorMessage: String? = null,
    val isOfflineMode: Boolean = false
)
