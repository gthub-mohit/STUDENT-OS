package com.studentos.core.intelligence.provider

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.intelligence.api.OmniRouteApiService
import com.studentos.core.intelligence.api.model.DeepSeekMessage
import com.studentos.core.intelligence.api.model.DeepSeekRequest
import com.studentos.core.intelligence.model.ConnectionTestResult
import com.studentos.core.intelligence.model.FailureReason
import com.studentos.core.intelligence.model.LLMResult
import com.studentos.core.intelligence.security.CredentialMigrator
import com.studentos.core.intelligence.security.SecureCredentialStorage
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OmniRouteProvider @Inject constructor(
    private val apiService: OmniRouteApiService,
    private val settingsDao: SettingsDao,
    private val secureStorage: SecureCredentialStorage
) : LLMProvider {

    override val name: String = "OmniRouteProvider"

    override suspend fun generateBrief(prompt: String): LLMResult {
        return executeCompletion(prompt, systemInstruction = "Generate structured daily brief guidance.")
    }

    override suspend fun updateGuidance(prompt: String): LLMResult {
        return executeCompletion(prompt, systemInstruction = "Update intra-day guidance for student context delta.")
    }

    suspend fun generateCompletion(prompt: String, systemInstruction: String? = null): LLMResult {
        return executeCompletion(
            prompt = prompt,
            systemInstruction = systemInstruction ?: "You are the Student OS personal growth and execution assistant."
        )
    }

    override fun isAvailable(): Boolean {
        // OmniRoute can run locally without a token, so availability is driven by whether AI is enabled
        return true
    }

    suspend fun getBaseUrl(): String {
        return settingsDao.get(KEY_OMNIROUTE_BASE_URL)?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
    }

    suspend fun getModel(): String {
        return settingsDao.get(KEY_OMNIROUTE_MODEL)?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
    }

    suspend fun getApiKey(): String? {
        return secureStorage.getCredential(CredentialMigrator.KEY_SECURE_OMNIROUTE_API_KEY)?.trim()?.takeIf { it.isNotBlank() }
    }

    private suspend fun executeCompletion(prompt: String, systemInstruction: String): LLMResult {
        val baseUrl = getBaseUrl().trimEnd('/')
        val model = getModel()
        val apiKey = getApiKey()
        val authHeader = if (!apiKey.isNullOrBlank()) "Bearer $apiKey" else null

        val endpointUrl = if (baseUrl.endsWith("/v1")) {
            "$baseUrl/chat/completions"
        } else if (baseUrl.contains("/v1/")) {
            "$baseUrl/chat/completions"
        } else {
            "$baseUrl/v1/chat/completions"
        }

        val request = DeepSeekRequest(
            model = model,
            messages = listOf(
                DeepSeekMessage(role = "system", content = systemInstruction),
                DeepSeekMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = apiService.generateChatCompletion(
                url = endpointUrl,
                authorization = authHeader,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val choiceContent = body?.choices?.firstOrNull()?.message?.content
                val totalTokens = body?.usage?.totalTokens ?: 0

                if (!choiceContent.isNullOrBlank()) {
                    LLMResult.Success(text = choiceContent, tokenCount = totalTokens)
                } else {
                    LLMResult.Failure(
                        reason = FailureReason.API_ERROR,
                        message = "Received empty response from OmniRoute gateway."
                    )
                }
            } else {
                when (response.code()) {
                    401, 403 -> LLMResult.Failure(
                        reason = FailureReason.INVALID_KEY,
                        message = "Invalid gateway credentials (HTTP ${response.code()})."
                    )
                    429 -> LLMResult.Failure(
                        reason = FailureReason.RATE_LIMITED,
                        message = "OmniRoute gateway rate limit exceeded (HTTP 429)."
                    )
                    504 -> LLMResult.Failure(
                        reason = FailureReason.TIMEOUT,
                        message = "OmniRoute gateway timeout (HTTP 504)."
                    )
                    else -> LLMResult.Failure(
                        reason = FailureReason.API_ERROR,
                        message = "HTTP ${response.code()}: ${response.message()}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            LLMResult.Failure(FailureReason.TIMEOUT, "Gateway request timed out: ${e.message}")
        } catch (e: UnknownHostException) {
            LLMResult.Failure(FailureReason.OFFLINE, "Gateway host unreachable: ${e.message}")
        } catch (e: ConnectException) {
            LLMResult.Failure(FailureReason.OFFLINE, "Failed to connect to OmniRoute: ${e.message}")
        } catch (e: SocketException) {
            LLMResult.Failure(FailureReason.OFFLINE, "Socket error connecting to gateway: ${e.message}")
        } catch (e: IOException) {
            LLMResult.Failure(FailureReason.API_ERROR, "Network I/O error: ${e.message}")
        }
    }

    /**
     * Probes the configured OmniRoute endpoint to verify connectivity and latency.
     * Does not expose secrets or tokens in the returned result.
     */
    suspend fun testConnection(
        overrideBaseUrl: String? = null,
        overrideApiKey: String? = null
    ): ConnectionTestResult {
        val baseUrl = (overrideBaseUrl ?: getBaseUrl()).trimEnd('/')
        val apiKey = overrideApiKey ?: getApiKey()
        val authHeader = if (!apiKey.isNullOrBlank()) "Bearer $apiKey" else null

        val healthUrl = if (baseUrl.endsWith("/v1")) {
            "$baseUrl/models"
        } else {
            "$baseUrl/v1/models"
        }

        val startTime = System.currentTimeMillis()

        return try {
            val response = apiService.checkHealth(
                url = healthUrl,
                authorization = authHeader
            )
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionTestResult.Success(
                    latencyMs = latency,
                    message = "Connected to OmniRoute (HTTP ${response.code()}, ${latency}ms)"
                )
            } else if (response.code() == 401 || response.code() == 403) {
                ConnectionTestResult.Failure(
                    reason = FailureReason.INVALID_KEY,
                    message = "Authentication failed (HTTP ${response.code()}). Check gateway token."
                )
            } else {
                // If /models returns 404, fallback to minimal ping on chat/completions
                testPingChatEndpoint(baseUrl, authHeader, startTime)
            }
        } catch (e: ConnectException) {
            ConnectionTestResult.Failure(
                reason = FailureReason.OFFLINE,
                message = "Connection refused. Verify OmniRoute is running on target port."
            )
        } catch (e: SocketTimeoutException) {
            ConnectionTestResult.Failure(
                reason = FailureReason.TIMEOUT,
                message = "Connection timed out after 15s. Check IP and firewall."
            )
        } catch (e: UnknownHostException) {
            ConnectionTestResult.Failure(
                reason = FailureReason.OFFLINE,
                message = "Unknown host. Check the configured IP address or domain."
            )
        } catch (e: Exception) {
            ConnectionTestResult.Failure(
                reason = FailureReason.API_ERROR,
                message = e.localizedMessage ?: "Unknown network error"
            )
        }
    }

    private suspend fun testPingChatEndpoint(
        baseUrl: String,
        authHeader: String?,
        startTime: Long
    ): ConnectionTestResult {
        val chatUrl = if (baseUrl.endsWith("/v1")) {
            "$baseUrl/chat/completions"
        } else {
            "$baseUrl/v1/chat/completions"
        }

        val pingRequest = DeepSeekRequest(
            model = getModel(),
            messages = listOf(DeepSeekMessage(role = "user", content = "ping")),
            temperature = 0.0
        )

        return try {
            val response = apiService.generateChatCompletion(
                url = chatUrl,
                authorization = authHeader,
                request = pingRequest
            )
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionTestResult.Success(
                    latencyMs = latency,
                    message = "Connected to OmniRoute (HTTP ${response.code()}, ${latency}ms)"
                )
            } else {
                ConnectionTestResult.Failure(
                    reason = FailureReason.API_ERROR,
                    message = "Gateway responded with HTTP ${response.code()}: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            ConnectionTestResult.Failure(
                reason = FailureReason.API_ERROR,
                message = e.localizedMessage ?: "Gateway ping failed"
            )
        }
    }

    companion object {
        const val KEY_OMNIROUTE_BASE_URL = "omniroute_base_url"
        const val KEY_OMNIROUTE_MODEL = "omniroute_model"
        const val DEFAULT_BASE_URL = "http://10.0.2.2:20128/v1/"
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}
