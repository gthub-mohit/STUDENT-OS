package com.studentos.core.intelligence.provider

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.intelligence.api.OmniRouteApiService
import com.studentos.core.intelligence.model.ConnectionTestResult
import com.studentos.core.intelligence.model.FailureReason
import com.studentos.core.intelligence.model.LLMResult
import com.studentos.core.intelligence.security.CredentialMigrator
import com.studentos.core.intelligence.security.SecureCredentialStorage
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class OmniRouteProviderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: OmniRouteApiService
    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val secureStorage: SecureCredentialStorage = mockk(relaxed = true)
    private lateinit var provider: OmniRouteProvider

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

        apiService = retrofit.create(OmniRouteApiService::class.java)
        provider = OmniRouteProvider(apiService, settingsDao, secureStorage)

        coEvery { settingsDao.get(OmniRouteProvider.KEY_OMNIROUTE_BASE_URL) } returns mockWebServer.url("/v1").toString()
        coEvery { settingsDao.get(OmniRouteProvider.KEY_OMNIROUTE_MODEL) } returns "deepseek-chat"
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_OMNIROUTE_API_KEY) } returns null
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun isAvailable_returnsTrueByDefault() {
        assertTrue(provider.isAvailable())
    }

    @Test
    fun generateBrief_returnsSuccess_onValidResponse() = runTest {
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_OMNIROUTE_API_KEY) } returns "gateway-token-123"

        val responseBody = """
            {
                "id": "omni-123",
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "Today's guidance: Review operating systems and DSA."
                        }
                    }
                ],
                "usage": {
                    "total_tokens": 85
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Success)
        val success = result as LLMResult.Success
        assertEquals("Today's guidance: Review operating systems and DSA.", success.text)
        assertEquals(85, success.tokenCount)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/v1/chat/completions", recordedRequest.path)
        assertEquals("Bearer gateway-token-123", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun generateBrief_omitsAuthHeader_whenNoTokenConfigured() = runTest {
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_OMNIROUTE_API_KEY) } returns null

        val responseBody = """
            {
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "Unauthenticated local response."
                        }
                    }
                ],
                "usage": { "total_tokens": 20 }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Success)
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun generateBrief_returnsFailureInvalidKey_onHttp401() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Failure)
        assertEquals(FailureReason.INVALID_KEY, (result as LLMResult.Failure).reason)
    }

    @Test
    fun generateBrief_returnsFailureRateLimited_onHttp429() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"rate_limited"}"""))

        val result = provider.generateBrief("Test prompt")

        assertTrue(result is LLMResult.Failure)
        assertEquals(FailureReason.RATE_LIMITED, (result as LLMResult.Failure).reason)
    }

    @Test
    fun testConnection_returnsSuccess_whenModelsEndpointSucceeds() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":[{"id":"deepseek-chat"}]}"""))

        val testResult = provider.testConnection()

        assertTrue(testResult is ConnectionTestResult.Success)
        val success = testResult as ConnectionTestResult.Success
        assertTrue(success.latencyMs >= 0)
        assertTrue(success.message.contains("HTTP 200"))

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/v1/models", recordedRequest.path)
    }

    @Test
    fun testConnection_fallsBackToChatPing_whenModelsEndpointIs404() = runTest {
        // Models returns 404
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        // Fallback chat/completions returns 200
        val chatResponse = """
            {
                "choices": [{"message": {"role": "assistant", "content": "pong"}}],
                "usage": {"total_tokens": 5}
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(chatResponse))


        val testResult = provider.testConnection()

        assertTrue(testResult is ConnectionTestResult.Success)
        val recorded1 = mockWebServer.takeRequest()
        val recorded2 = mockWebServer.takeRequest()
        assertEquals("/v1/models", recorded1.path)
        assertEquals("/v1/chat/completions", recorded2.path)
    }

    @Test
    fun testConnection_returnsFailure_onHttp401_withoutExposingSecrets() = runTest {
        coEvery { secureStorage.getCredential(CredentialMigrator.KEY_SECURE_OMNIROUTE_API_KEY) } returns "secret-key-to-not-leak"

        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        val testResult = provider.testConnection()

        assertTrue(testResult is ConnectionTestResult.Failure)
        val failure = testResult as ConnectionTestResult.Failure
        assertEquals(FailureReason.INVALID_KEY, failure.reason)
        assertFalse(failure.message.contains("secret-key-to-not-leak"))
    }

    @Test
    fun testConnection_returnsFailure_whenServerIsUnreachable() = runTest {
        mockWebServer.shutdown()

        val testResult = provider.testConnection()

        assertTrue(testResult is ConnectionTestResult.Failure)
        val failure = testResult as ConnectionTestResult.Failure
        assertTrue(failure.reason == FailureReason.OFFLINE || failure.reason == FailureReason.API_ERROR)
    }
}
