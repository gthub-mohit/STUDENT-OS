package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.intelligence.security.CredentialMigrator
import com.studentos.core.intelligence.security.SecureCredentialStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekKeyProvider @Inject constructor(
    private val settingsDao: SettingsDao,
    private val secureStorage: SecureCredentialStorage
) {
    suspend fun getApiKey(): String? {
        val secureKey = secureStorage.getCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY)?.trim()
        if (!secureKey.isNullOrBlank()) {
            return secureKey
        }
        return settingsDao.get(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY)?.trim()?.takeIf { it.isNotBlank() }
    }
}

