package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMProviderFactory @Inject constructor(
    private val settingsDao: SettingsDao,
    private val mockProvider: MockProvider,
    private val deepSeekProvider: DeepSeekProvider
) {
    suspend fun getProvider(): LLMProvider {
        val providerSetting = settingsDao.get("ai_provider") ?: PROVIDER_MOCK
        return when (providerSetting.uppercase()) {
            PROVIDER_DEEPSEEK -> deepSeekProvider
            PROVIDER_MOCK -> mockProvider
            else -> mockProvider
        }
    }

    companion object {
        const val PROVIDER_MOCK = "MOCK"
        const val PROVIDER_DEEPSEEK = "DEEPSEEK"
    }
}
