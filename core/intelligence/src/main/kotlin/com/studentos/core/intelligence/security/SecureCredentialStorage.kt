package com.studentos.core.intelligence.security

/**
 * SecureCredentialStorage — Hardware-backed credential storage abstraction.
 */
interface SecureCredentialStorage {
    suspend fun saveCredential(key: String, value: String): Boolean
    suspend fun getCredential(key: String): String?
    suspend fun removeCredential(key: String): Boolean
    suspend fun hasCredential(key: String): Boolean
}
