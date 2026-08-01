package com.studentos.feature.coding.presentation.state

data class ContestReflectionUiState(
    val contestId: Long = 0L,
    val wentWrong: String = "",
    val toRevise: String = "",
    val selfRating: Int = 3,
    val existingReflectionId: Long = 0L,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val errorMessage: String? = null
)
