package com.studentos.feature.settings.data

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
import com.studentos.core.intelligence.security.CredentialMigrator
import com.studentos.core.intelligence.security.SecureCredentialStorage
import com.studentos.feature.settings.data.repository.SettingsRepositoryImpl
import com.studentos.feature.settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryImplTest {

    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val secureStorage: SecureCredentialStorage = mockk(relaxed = true)
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        repository = SettingsRepositoryImpl(settingsDao, secureStorage)
    }

    @Test
    fun getAttendanceThreshold_returnsDefaultWhenNotSet() = runTest {
        coEvery { settingsDao.get(SettingsRepository.KEY_ATTENDANCE_THRESHOLD) } returns null

        val result = repository.getAttendanceThreshold()
        assertEquals(SettingsRepository.DEFAULT_ATTENDANCE_THRESHOLD, result)
    }

    @Test
    fun setAttendanceThreshold_persistsSettingEntity() = runTest {
        repository.setAttendanceThreshold(80)

        coVerify {
            settingsDao.set(
                match { it.key == SettingsRepository.KEY_ATTENDANCE_THRESHOLD && it.value == "80" }
            )
        }
    }

    @Test
    fun getAiEnabled_returnsParsedBoolean() = runTest {
        coEvery { settingsDao.get(SettingsRepository.KEY_AI_ENABLED) } returns "false"

        val result = repository.getAiEnabled()
        assertEquals(false, result)
    }

    @Test
    fun getOmniRouteBaseUrl_returnsDefaultWhenNotSet() = runTest {
        coEvery { settingsDao.get(SettingsRepository.KEY_OMNIROUTE_BASE_URL) } returns null

        val result = repository.getOmniRouteBaseUrl()
        assertEquals(SettingsRepository.DEFAULT_OMNIROUTE_BASE_URL, result)
    }

    @Test
    fun setOmniRouteBaseUrl_persistsSettingEntity() = runTest {
        repository.setOmniRouteBaseUrl("http://192.168.1.50:20128/v1/")

        coVerify {
            settingsDao.set(
                match { it.key == SettingsRepository.KEY_OMNIROUTE_BASE_URL && it.value == "http://192.168.1.50:20128/v1/" }
            )
        }
    }

    @Test
    fun setOmniRouteApiKey_savesToSecureStorage_doesNotWriteToDao() = runTest {
        repository.setOmniRouteApiKey("omni-secure-secret")

        coVerify {
            secureStorage.saveCredential(CredentialMigrator.KEY_SECURE_OMNIROUTE_API_KEY, "omni-secure-secret")
        }
        coVerify(exactly = 0) {
            settingsDao.set(match { it.key == SettingsRepository.KEY_OMNIROUTE_API_KEY })
        }
    }

    @Test
    fun setDeepSeekApiKey_savesToSecureStorage_andDeletesFromDao() = runTest {
        repository.setDeepSeekApiKey("sk-secret-key-123")

        coVerify {
            secureStorage.saveCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY, "sk-secret-key-123")
            settingsDao.delete(SettingsRepository.KEY_DEEPSEEK_API_KEY)
        }
    }

    @Test
    fun reset_resetsAllSettingsToDefaults() = runTest {
        repository.reset()

        coVerify {
            settingsDao.set(match { it.key == SettingsRepository.KEY_ATTENDANCE_THRESHOLD && it.value == "75" })
            settingsDao.set(match { it.key == SettingsRepository.KEY_AI_PROVIDER && it.value == "OMNIROUTE" })
            settingsDao.set(match { it.key == SettingsRepository.KEY_OMNIROUTE_BASE_URL && it.value == "http://10.0.2.2:20128/v1/" })
            settingsDao.set(match { it.key == SettingsRepository.KEY_DAILY_BRIEF_TIME && it.value == "07:00" })
        }
    }

    @Test
    fun observeAllSettings_mapsDaoListToDomain() = runTest {
        val entities = listOf(
            SettingEntity(SettingsRepository.KEY_ATTENDANCE_THRESHOLD, "85"),
            SettingEntity(SettingsRepository.KEY_HANDLE_CODECHEF, "tourist"),
            SettingEntity(SettingsRepository.KEY_AI_ENABLED, "true"),
            SettingEntity(SettingsRepository.KEY_OMNIROUTE_BASE_URL, "http://127.0.0.1:20128/v1/"),
            SettingEntity(SettingsRepository.KEY_OMNIROUTE_MODEL, "llama3")
        )
        coEvery { settingsDao.observeAll() } returns flowOf(entities)
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_OMNIROUTE_API_KEY) } returns "omni-tok"

        val flow = repository.observeAllSettings()
        flow.collect { domain ->
            assertEquals(85, domain.attendanceThreshold)
            assertEquals("tourist", domain.codeChefHandle)
            assertTrue(domain.aiEnabled)
            assertEquals("http://127.0.0.1:20128/v1/", domain.omniRouteBaseUrl)
            assertEquals("llama3", domain.omniRouteModel)
            assertEquals("omni-tok", domain.omniRouteApiKey)
        }
    }
}

