package com.studentos.core.intelligence.security

import com.studentos.core.database.dao.SettingsDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CredentialMigrator — Safe, verified one-time migration of plaintext SQLite credentials into
 * hardware-backed SecureCredentialStorage.
 *
 * Safety guarantees:
 *  - Distinguishes legacy DeepSeek credentials from OmniRoute tokens.
 *  - Verifies read-back decryption before deleting plaintext SQLite records.
 *  - On failure, aborts deletion so the user never loses their credential.
 */
@Singleton
class CredentialMigrator @Inject constructor(
    private val settingsDao: SettingsDao,
    private val secureStorage: SecureCredentialStorage
) {

    suspend fun migrateLegacyCredentials(): MigrationResult {
        val legacyKey = settingsDao.get(KEY_LEGACY_DEEPSEEK_API_KEY)?.trim()
        if (legacyKey.isNullOrBlank()) {
            return MigrationResult.NoActionRequired
        }

        // If already present and verified in secure storage, safe to purge legacy plaintext copy
        val existingSecure = secureStorage.getCredential(KEY_SECURE_DEEPSEEK_API_KEY)
        if (existingSecure == legacyKey) {
            settingsDao.delete(KEY_LEGACY_DEEPSEEK_API_KEY)
            return MigrationResult.AlreadyMigrated
        }

        // Step 1: Encrypt and persist into secure storage
        val saveSuccess = secureStorage.saveCredential(KEY_SECURE_DEEPSEEK_API_KEY, legacyKey)
        if (!saveSuccess) {
            return MigrationResult.Failed("Failed to encrypt and store credential into hardware keystore.")
        }

        // Step 2: Verify read-back before modifying SQLite
        val readBack = secureStorage.getCredential(KEY_SECURE_DEEPSEEK_API_KEY)
        if (readBack != legacyKey) {
            // Read-back verification failed: do NOT delete SQLite copy
            return MigrationResult.Failed("Decryption verification failed. Plaintext record preserved.")
        }

        // Step 3: Verified! Safe to delete plaintext copy
        settingsDao.delete(KEY_LEGACY_DEEPSEEK_API_KEY)
        return MigrationResult.Success
    }

    companion object {
        const val KEY_LEGACY_DEEPSEEK_API_KEY = "deepseek_api_key"
        const val KEY_SECURE_DEEPSEEK_API_KEY = "deepseek_api_key"
        const val KEY_SECURE_OMNIROUTE_API_KEY = "omniroute_api_key"
    }

    sealed interface MigrationResult {
        object NoActionRequired : MigrationResult
        object AlreadyMigrated : MigrationResult
        object Success : MigrationResult
        data class Failed(val reason: String) : MigrationResult
    }
}
