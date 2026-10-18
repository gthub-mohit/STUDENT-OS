package com.studentos.feature.settings.presentation.state

import com.studentos.feature.settings.domain.model.SettingsDomain

sealed interface ConnectionTestState {
    object Idle : ConnectionTestState
    object Testing : ConnectionTestState
    data class Success(val latencyMs: Long, val message: String) : ConnectionTestState
    data class Failure(val message: String) : ConnectionTestState
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val settings: SettingsDomain = SettingsDomain(),
    val isApiKeyVisible: Boolean = false,
    val connectionTestState: ConnectionTestState = ConnectionTestState.Idle,
    val userMessage: String? = null,
    val showResetConfirmDialog: Boolean = false,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportJson: String? = null,
    val showTimePickerDialog: Boolean = false
)

