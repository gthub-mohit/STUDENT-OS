package com.studentos.core.sync.api

import com.studentos.core.sync.api.dto.CodeChefContestDto
import com.studentos.core.sync.api.dto.CodeChefProfileResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CodeChefApiServiceImpl @Inject constructor(
    @Named("cp") private val okHttpClient: OkHttpClient
) : CodeChefApiService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Serializable
    private data class RawCodeChefContest(
        val code: String? = null,
        val name: String? = null,
        val rating: String? = null,
        val rank: String? = null,
        val end_date: String? = null,
        val getyear: String? = null,
        val getmonth: String? = null,
        val getday: String? = null
    )

    override suspend fun getUserProfile(handle: String): CodeChefProfileResponseDto = withContext(Dispatchers.IO) {
        val cleanHandle = handle.trim()
        val request = Request.Builder()
            .url("https://www.codechef.com/users/$cleanHandle")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("CodeChef HTTP error: ${response.code}")
        }

        val html = response.body?.string() ?: ""

        // 1. Extract date_versus_rating JSON array from Drupal.settings
        val contests = mutableListOf<CodeChefContestDto>()
        val dateVersusRatingRegex = """"date_versus_rating"\s*:\s*\{\s*"all"\s*:\s*(\[[^\]]*\]|\[[\s\S]*?\](?=,\s*"all_old"|\}\s*,\s*"maxSetMembersCount"|\}\s*\}|\}\s*\)\s*;))""".toRegex()
        val match = dateVersusRatingRegex.find(html)

        if (match != null) {
            val jsonArrayStr = match.groupValues[1]
            try {
                val rawContests = json.decodeFromString<List<RawCodeChefContest>>(jsonArrayStr)
                var prevRating: Int? = null

                for (rc in rawContests) {
                    val r = rc.rating?.toIntOrNull()
                    val rk = rc.rank?.toIntOrNull()
                    val change = if (r != null && prevRating != null) r - prevRating else null
                    if (r != null) prevRating = r

                    contests.add(
                        CodeChefContestDto(
                            code = rc.code,
                            name = rc.name,
                            rank = rk,
                            rating = r,
                            ratingChange = change,
                            endDate = rc.end_date
                        )
                    )
                }
            } catch (_: Exception) {
                // Ignore parse errors on malformed scripts
            }
        }

        // 2. Extract rating from contests or fallback to HTML regex
        val ratingFromContests = contests.lastOrNull { it.rating != null }?.rating
        val ratingFromHtml = """class=["']rating-number["'][^>]*>(\d+)""".toRegex().find(html)?.groupValues?.get(1)?.toIntOrNull()
        val currentRating = ratingFromContests ?: ratingFromHtml

        // 3. Extract stars
        val stars = when {
            currentRating == null -> null
            currentRating < 1400 -> "1★"
            currentRating < 1600 -> "2★"
            currentRating < 1800 -> "3★"
            currentRating < 2000 -> "4★"
            currentRating < 2200 -> "5★"
            currentRating < 2500 -> "6★"
            else -> "7★"
        }

        CodeChefProfileResponseDto(
            status = "success",
            success = true,
            handle = cleanHandle,
            currentRating = currentRating,
            rating = currentRating,
            stars = stars,
            ratingData = contests
        )
    }
}
