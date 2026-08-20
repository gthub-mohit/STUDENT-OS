package com.studentos.feature.settings.presentation.state

import com.studentos.feature.settings.domain.model.SettingsDomain

data class SettingsUiState(
    val isLoading: Boolean = false,
    val settings: SettingsDomain = SettingsDomain(),
    val isApiKeyVisible: Boolean = false,
    val userMessage: String? = null,
    val showResetConfirmDialog: Boolean = false,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportJson: String? = null,
    val showTimePickerDialog: Boolean = false
)
