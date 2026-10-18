package com.studentos.core.intelligence.model

/**
 * ConnectionTestResult — Status of an explicit test probe to the OmniRoute gateway.
 */
sealed class ConnectionTestResult {
    data class Success(
        val latencyMs: Long,
        val message: String = "Connected successfully"
    ) : ConnectionTestResult()

    data class Failure(
        val reason: FailureReason,
        val message: String
    ) : ConnectionTestResult()
}
