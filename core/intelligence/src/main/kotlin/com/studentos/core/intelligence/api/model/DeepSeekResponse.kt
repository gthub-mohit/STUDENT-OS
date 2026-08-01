package com.studentos.core.intelligence.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepSeekResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("choices") val choices: List<DeepSeekChoice> = emptyList(),
    @SerialName("usage") val usage: DeepSeekUsage? = null
)

@Serializable
data class DeepSeekChoice(
    @SerialName("message") val message: DeepSeekMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class DeepSeekUsage(
    @SerialName("total_tokens") val totalTokens: Int = 0
)
