package com.studentos.core.events

/**
 * AppResult — Result wrapper for use cases and repositories (folder-structure.md §9.5).
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val reason: AppError) : AppResult<Nothing>()
}

/**
 * AppError — Standardized error sealed hierarchy (folder-structure.md §9.5).
 */
sealed class AppError {
    data class DatabaseError(val message: String) : AppError()
    data class NetworkError(val message: String) : AppError()
    data class ValidationError(val message: String) : AppError()
    object Offline : AppError()
    object RateLimited : AppError()
}
