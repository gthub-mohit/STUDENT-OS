package com.studentos.core.intelligence.provider

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.studentos.core.intelligence.api.DeepSeekApiService
import com.studentos.core.intelligence.model.FailureReason
import com.studentos.core.intelligence.model.LLMResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class DeepSeekProviderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: DeepSeekApiService
    private val keyProvider: DeepSeekKeyProvider = mockk()
    private lateinit var provider: DeepSeekProvider

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        apiService = retrofit.create(DeepSeekApiService::class.java)
        provider = DeepSeekProvider(apiService, keyProvider)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun isAvailable_returnsFalse_whenKeyIsMissing() {
        coEvery { keyProvider.getApiKey() } returns null
        assertFalse(provider.isAvailable())
    }

    @Test
    fun isAvailable_returnsTrue_whenKeyIsPresent() {
        coEvery { keyProvider.getApiKey() } returns "sk-valid-key"
        assertTrue(provider.isAvailable())
    }

    @Test
    fun generateBrief_returnsSuccess_onValidResponse() = runTest {
        coEvery { keyProvider.getApiKey() } returns "sk-valid-key"

        val responseBody = """
            {
                "id": "chatcmpl-123",
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "Focus on Algorithms and Assignment 2 today."
                        }
                    }
                ],
                "usage": {
                    "total_tokens": 120
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Success)
        val success = result as LLMResult.Success
        assertEquals("Focus on Algorithms and Assignment 2 today.", success.text)
        assertEquals(120, success.tokenCount)
    }

    @Test
    fun generateBrief_returnsFailureInvalidKey_onHttp401() = runTest {
        coEvery { keyProvider.getApiKey() } returns "sk-invalid-key"

        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"invalid_api_key"}"""))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Failure)
        val failure = result as LLMResult.Failure
        assertEquals(FailureReason.INVALID_KEY, failure.reason)
    }

    @Test
    fun generateBrief_returnsFailureRateLimited_onHttp429() = runTest {
        coEvery { keyProvider.getApiKey() } returns "sk-valid-key"

        mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"rate_limit_exceeded"}"""))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Failure)
        val failure = result as LLMResult.Failure
        assertEquals(FailureReason.RATE_LIMITED, failure.reason)
    }

    @Test
    fun generateBrief_returnsFailureApiError_onHttp500() = runTest {
        coEvery { keyProvider.getApiKey() } returns "sk-valid-key"

        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"internal_server_error"}"""))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Failure)
        val failure = result as LLMResult.Failure
        assertEquals(FailureReason.API_ERROR, failure.reason)
    }
}
