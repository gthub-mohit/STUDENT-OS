package com.studentos.core.intelligence.provider

import com.studentos.core.intelligence.api.DeepSeekApiService
import com.studentos.core.intelligence.api.model.DeepSeekMessage
import com.studentos.core.intelligence.api.model.DeepSeekRequest
import com.studentos.core.intelligence.model.FailureReason
import com.studentos.core.intelligence.model.LLMResult
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekProvider @Inject constructor(
    private val apiService: DeepSeekApiService,
    private val keyProvider: DeepSeekKeyProvider
) : LLMProvider {

    override val name: String = "DeepSeekProvider"

    override suspend fun generateBrief(prompt: String): LLMResult {
        return executeCompletion(prompt, systemInstruction = "Generate structured daily brief guidance.")
    }

    override suspend fun updateGuidance(prompt: String): LLMResult {
        return executeCompletion(prompt, systemInstruction = "Update intra-day guidance for student context delta.")
    }

    override fun isAvailable(): Boolean {
        val key = runBlocking { keyProvider.getApiKey() }
        return !key.isNullOrBlank()
    }

    private suspend fun executeCompletion(prompt: String, systemInstruction: String): LLMResult {
        val apiKey = keyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return LLMResult.Failure(
                reason = FailureReason.INVALID_KEY,
                message = "DeepSeek API key is missing or invalid."
            )
        }

        val request = DeepSeekRequest(
            messages = listOf(
                DeepSeekMessage(role = "system", content = systemInstruction),
                DeepSeekMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = apiService.generateChatCompletion(
                authorization = "Bearer $apiKey",
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
                        message = "Received empty response from DeepSeek API."
                    )
                }
            } else {
                when (response.code()) {
                    401, 403 -> LLMResult.Failure(
                        reason = FailureReason.INVALID_KEY,
                        message = "Invalid API Key (HTTP ${response.code()})."
                    )
                    429 -> LLMResult.Failure(
                        reason = FailureReason.RATE_LIMITED,
                        message = "DeepSeek API rate limit exceeded (HTTP 429)."
                    )
                    504 -> LLMResult.Failure(
                        reason = FailureReason.TIMEOUT,
                        message = "DeepSeek gateway timeout (HTTP 504)."
                    )
                    else -> LLMResult.Failure(
                        reason = FailureReason.API_ERROR,
                        message = "HTTP ${response.code()}: ${response.message()}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            LLMResult.Failure(FailureReason.TIMEOUT, "Request timed out: ${e.message}")
        } catch (e: UnknownHostException) {
            LLMResult.Failure(FailureReason.OFFLINE, "No network connection: ${e.message}")
        } catch (e: ConnectException) {
            LLMResult.Failure(FailureReason.OFFLINE, "Connection failed: ${e.message}")
        } catch (e: SocketException) {
            LLMResult.Failure(FailureReason.OFFLINE, "Socket error: ${e.message}")
        } catch (e: IOException) {
            LLMResult.Failure(FailureReason.OFFLINE, "Network error: ${e.message}")
        } catch (e: Exception) {
            LLMResult.Failure(FailureReason.API_ERROR, "Unexpected error: ${e.message}")
        }
    }
}
