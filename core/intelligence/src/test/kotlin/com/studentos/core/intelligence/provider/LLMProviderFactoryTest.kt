package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LLMProviderFactoryTest {

    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val deepSeekProvider: DeepSeekProvider = mockk()
    private lateinit var factory: LLMProviderFactory

    @Before
    fun setUp() {
        factory = LLMProviderFactory(settingsDao, deepSeekProvider)
    }

    @Test
    fun getProvider_migratesLegacyMockSetting_andReturnsDeepSeekProvider() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns "MOCK"
        coEvery { deepSeekProvider.name } returns "DeepSeekProvider"

        val provider = factory.getProvider()

        assertEquals("DeepSeekProvider", provider.name)
        coVerify { settingsDao.set(SettingEntity("ai_provider", "DEEPSEEK")) }
    }

    @Test
    fun getProvider_returnsDeepSeekProvider_whenSettingIsDeepSeek() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns "DEEPSEEK"
        coEvery { deepSeekProvider.name } returns "DeepSeekProvider"

        val provider = factory.getProvider()

        assertEquals("DeepSeekProvider", provider.name)
    }

    @Test
    fun getProvider_returnsDeepSeekProvider_whenSettingIsNull() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns null
        coEvery { deepSeekProvider.name } returns "DeepSeekProvider"

        val provider = factory.getProvider()

        assertEquals("DeepSeekProvider", provider.name)
    }
}

