package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMProviderFactory @Inject constructor(
    private val settingsDao: SettingsDao,
    private val deepSeekProvider: DeepSeekProvider,
    private val omniRouteProvider: OmniRouteProvider
) {
    suspend fun getProvider(): LLMProvider {
        val providerSetting = settingsDao.get("ai_provider")
        return when {
            providerSetting.equals(PROVIDER_DEEPSEEK, ignoreCase = true) -> deepSeekProvider
            providerSetting.equals(PROVIDER_MOCK, ignoreCase = true) -> deepSeekProvider
            providerSetting.equals(PROVIDER_OMNIROUTE, ignoreCase = true) -> omniRouteProvider
            else -> omniRouteProvider
        }
    }

    companion object {
        const val PROVIDER_MOCK = "MOCK"
        const val PROVIDER_DEEPSEEK = "DEEPSEEK"
        const val PROVIDER_OMNIROUTE = "OMNIROUTE"
    }
}


