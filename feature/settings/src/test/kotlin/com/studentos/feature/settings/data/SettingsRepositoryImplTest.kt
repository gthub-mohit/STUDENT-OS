package com.studentos.feature.settings.data

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
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
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        repository = SettingsRepositoryImpl(settingsDao)
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
    fun reset_resetsAllSettingsToDefaults() = runTest {
        repository.reset()

        coVerify {
            settingsDao.set(match { it.key == SettingsRepository.KEY_ATTENDANCE_THRESHOLD && it.value == "75" })
            settingsDao.set(match { it.key == SettingsRepository.KEY_AI_PROVIDER && it.value == "DEEPSEEK" })
            settingsDao.set(match { it.key == SettingsRepository.KEY_DAILY_BRIEF_TIME && it.value == "07:00" })
        }
    }

    @Test
    fun observeAllSettings_mapsDaoListToDomain() = runTest {
        val entities = listOf(
            SettingEntity(SettingsRepository.KEY_ATTENDANCE_THRESHOLD, "85"),
            SettingEntity(SettingsRepository.KEY_HANDLE_CODECHEF, "tourist"),
            SettingEntity(SettingsRepository.KEY_AI_ENABLED, "true")
        )
        coEvery { settingsDao.observeAll() } returns flowOf(entities)

        val flow = repository.observeAllSettings()
        flow.collect { domain ->
            assertEquals(85, domain.attendanceThreshold)
            assertEquals("tourist", domain.codeChefHandle)
            assertTrue(domain.aiEnabled)
        }
    }
}
