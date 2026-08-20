package com.studentos.feature.settings.domain.repository

import com.studentos.feature.settings.domain.model.StudentOsBackup

interface BackupRepository {
    suspend fun createBackup(): StudentOsBackup
    suspend fun exportBackupJson(): String
    suspend fun restoreBackup(backup: StudentOsBackup): Result<Unit>
    suspend fun restoreBackupJson(jsonString: String): Result<Unit>
}
