package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekKeyProvider @Inject constructor(
    private val settingsDao: SettingsDao
) {
    suspend fun getApiKey(): String? {
        return settingsDao.get("deepseek_api_key")?.trim()?.takeIf { it.isNotBlank() }
    }
}
