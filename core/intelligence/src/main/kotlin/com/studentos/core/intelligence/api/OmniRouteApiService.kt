package com.studentos.core.intelligence.api

import com.studentos.core.intelligence.api.model.DeepSeekRequest
import com.studentos.core.intelligence.api.model.DeepSeekResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * OmniRouteApiService — Retrofit interface for OmniRoute and OpenAI-compatible gateways.
 *
 * Uses dynamic `@Url` parameter to support user-configured endpoints (emulator loopback,
 * adb reverse tunnel, LAN IP, or hosted HTTPS server) without rebuilding the Retrofit instance.
 */
interface OmniRouteApiService {

    @POST
    suspend fun generateChatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: DeepSeekRequest
    ): Response<DeepSeekResponse>

    @GET
    suspend fun checkHealth(
        @Url url: String,
        @Header("Authorization") authorization: String?
    ): Response<ResponseBody>
}
