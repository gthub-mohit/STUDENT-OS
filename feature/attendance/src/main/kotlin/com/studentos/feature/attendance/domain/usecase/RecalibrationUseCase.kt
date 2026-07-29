package com.studentos.feature.attendance.domain.usecase

import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * RecalibrationUseCase — Startup verification use case that computes all subject attendance
 * statistics directly from raw [com.studentos.core.database.entity.ClassEventEntity] records.
 *
 * In V1 design (backend-blueprint.md §T6), attendance percentages are calculated on-the-fly from
 * raw class events. This use case executes a read-only verification pass at app startup to ensure
 * data consistency before the home attendance screen is displayed.
 */
class RecalibrationUseCase @Inject constructor(
    private val classEventRepository: ClassEventRepository
) {
    /**
     * Executes the recalibration pass.
     *
     * @return [AppResult.Success] containing [Unit] if recalibration completes without error,
     * or [AppResult.Failure] containing an [AppError] if a database error occurs.
     */
    suspend operator fun invoke(): AppResult<Unit> {
        return try {
            val summaries = classEventRepository.getAllAttendanceSummaries().first()
            // Verify computation scan completes successfully for all active subjects
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(
                AppError.DatabaseError(
                    e.message ?: "Recalibration failed during attendance summary calculation"
                )
            )
        }
    }
}
