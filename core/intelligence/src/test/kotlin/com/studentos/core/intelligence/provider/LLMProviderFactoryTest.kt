package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LLMProviderFactoryTest {

    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val deepSeekProvider: DeepSeekProvider = mockk()
    private val omniRouteProvider: OmniRouteProvider = mockk()
    private lateinit var factory: LLMProviderFactory

    @Before
    fun setUp() {
        factory = LLMProviderFactory(settingsDao, deepSeekProvider, omniRouteProvider)
    }

    @Test
    fun getProvider_returnsDeepSeekProvider_whenSettingIsMock() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns "MOCK"
        coEvery { deepSeekProvider.name } returns "DeepSeekProvider"

        val provider = factory.getProvider()

        assertEquals("DeepSeekProvider", provider.name)
    }

    @Test
    fun getProvider_returnsDeepSeekProvider_whenSettingIsDeepSeek() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns "DEEPSEEK"
        coEvery { deepSeekProvider.name } returns "DeepSeekProvider"

        val provider = factory.getProvider()

        assertEquals("DeepSeekProvider", provider.name)
    }

    @Test
    fun getProvider_returnsOmniRouteProvider_whenSettingIsOmniRoute() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns "OMNIROUTE"
        coEvery { omniRouteProvider.name } returns "OmniRouteProvider"

        val provider = factory.getProvider()

        assertEquals("OmniRouteProvider", provider.name)
    }

    @Test
    fun getProvider_returnsOmniRouteProvider_whenSettingIsNull() = runTest {
        coEvery { settingsDao.get("ai_provider") } returns null
        coEvery { omniRouteProvider.name } returns "OmniRouteProvider"

        val provider = factory.getProvider()

        assertEquals("OmniRouteProvider", provider.name)
    }
}


