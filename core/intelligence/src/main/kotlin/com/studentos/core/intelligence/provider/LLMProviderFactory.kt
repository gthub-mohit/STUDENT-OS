package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMProviderFactory @Inject constructor(
    private val settingsDao: SettingsDao,
    private val deepSeekProvider: DeepSeekProvider
) {
    suspend fun getProvider(): LLMProvider {
        val providerSetting = settingsDao.get("ai_provider")
        if (providerSetting != null && providerSetting.equals(PROVIDER_MOCK, ignoreCase = true)) {
            settingsDao.set(SettingEntity("ai_provider", PROVIDER_DEEPSEEK))
        }
        return deepSeekProvider
    }

    companion object {
        const val PROVIDER_MOCK = "MOCK"
        const val PROVIDER_DEEPSEEK = "DEEPSEEK"
    }
}

