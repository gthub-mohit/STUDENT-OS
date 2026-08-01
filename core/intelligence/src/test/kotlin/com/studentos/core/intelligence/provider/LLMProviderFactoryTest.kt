package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LLMProviderFactoryTest {

    private val settingsDao: SettingsDao = mockk()
    private val mockProvider: MockProvider = MockProvider()
    private val deepSeekProvider: DeepSeekProvider = mockk()
    private lateinit var factory: LLMProviderFactory

    @Before
    fun setUp() {
        factory = LLMProviderFactory(settingsDao, mockProvider, deepSeekProvider)
    }

    @Test
    fun getProvider_returnsMockProvider_whenSettingIsMock() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns "MOCK"

        val provider = factory.getProvider()

        assertEquals("MockProvider", provider.name)
    }

    @Test
    fun getProvider_returnsDeepSeekProvider_whenSettingIsDeepSeek() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns "DEEPSEEK"
        coEvery { deepSeekProvider.name } returns "DeepSeekProvider"

        val provider = factory.getProvider()

        assertEquals("DeepSeekProvider", provider.name)
    }

    @Test
    fun getProvider_returnsMockProvider_whenSettingIsNull() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns null

        val provider = factory.getProvider()

        assertEquals("MockProvider", provider.name)
    }
}
