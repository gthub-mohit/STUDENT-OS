package com.studentos.core.intelligence.security

import com.studentos.core.database.dao.SettingsDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CredentialMigratorTest {

    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val secureStorage: SecureCredentialStorage = mockk(relaxed = true)
    private lateinit var migrator: CredentialMigrator

    @Before
    fun setUp() {
        migrator = CredentialMigrator(settingsDao, secureStorage)
    }

    @Test
    fun `when no legacy key exists, returns NoActionRequired and does not modify storage`() = runTest {
        coEvery { settingsDao.get(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) } returns null

        val result = migrator.migrateLegacyCredentials()

        assertEquals(CredentialMigrator.MigrationResult.NoActionRequired, result)
        coVerify(exactly = 0) { secureStorage.saveCredential(any(), any()) }
        coVerify(exactly = 0) { settingsDao.delete(any()) }
    }

    @Test
    fun `when legacy key is blank, returns NoActionRequired`() = runTest {
        coEvery { settingsDao.get(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) } returns "   "

        val result = migrator.migrateLegacyCredentials()

        assertEquals(CredentialMigrator.MigrationResult.NoActionRequired, result)
        coVerify(exactly = 0) { settingsDao.delete(any()) }
    }

    @Test
    fun `when already migrated, cleans up legacy copy and returns AlreadyMigrated`() = runTest {
        val key = "sk-test-key-123"
        coEvery { settingsDao.get(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) } returns key
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY) } returns key

        val result = migrator.migrateLegacyCredentials()

        assertEquals(CredentialMigrator.MigrationResult.AlreadyMigrated, result)
        coVerify(exactly = 1) { settingsDao.delete(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) }
    }

    @Test
    fun `when legacy key exists and save and verification succeed, deletes legacy copy and returns Success`() = runTest {
        val key = "sk-deepseek-secret"
        coEvery { settingsDao.get(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) } returns key
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY) } returnsMany listOf(null, key)
        coEvery { secureStorage.saveCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY, key) } returns true

        val result = migrator.migrateLegacyCredentials()

        assertEquals(CredentialMigrator.MigrationResult.Success, result)
        coVerify(exactly = 1) { secureStorage.saveCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY, key) }
        coVerify(exactly = 1) { settingsDao.delete(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) }
    }

    @Test
    fun `when secure save fails, does NOT delete plaintext copy`() = runTest {
        val key = "sk-deepseek-secret"
        coEvery { settingsDao.get(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) } returns key
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY) } returns null
        coEvery { secureStorage.saveCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY, key) } returns false

        val result = migrator.migrateLegacyCredentials()

        assertTrue(result is CredentialMigrator.MigrationResult.Failed)
        coVerify(exactly = 0) { settingsDao.delete(any()) }
    }

    @Test
    fun `when read-back verification mismatches, does NOT delete plaintext copy`() = runTest {
        val key = "sk-deepseek-secret"
        coEvery { settingsDao.get(CredentialMigrator.KEY_LEGACY_DEEPSEEK_API_KEY) } returns key
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY) } returnsMany listOf(null, "corrupted-or-empty")
        coEvery { secureStorage.saveCredential(CredentialMigrator.KEY_SECURE_DEEPSEEK_API_KEY, key) } returns true

        val result = migrator.migrateLegacyCredentials()

        assertTrue(result is CredentialMigrator.MigrationResult.Failed)
        coVerify(exactly = 0) { settingsDao.delete(any()) }
    }
}
