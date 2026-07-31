package com.studentos.core.sync.api

import com.studentos.core.sync.api.dto.CodeforcesRatingResponseDto
import com.studentos.core.sync.api.dto.CodeforcesUserResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CodeforcesApiService {

    @GET("api/user.info")
    suspend fun getUserInfo(
        @Query("handles") handle: String
    ): CodeforcesUserResponseDto

    @GET("api/user.rating")
    suspend fun getUserRating(
        @Query("handle") handle: String
    ): CodeforcesRatingResponseDto
}
