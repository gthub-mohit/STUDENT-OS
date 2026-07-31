package com.studentos.core.sync.api

import com.studentos.core.sync.api.dto.CodeChefProfileResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface CodeChefApiService {

    @GET("users/{handle}")
    suspend fun getUserProfile(
        @Path("handle") handle: String
    ): CodeChefProfileResponseDto
}
