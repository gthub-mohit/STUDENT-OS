package com.studentos.feature.intelligence.domain.usecase

import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.model.GoalValidationResult
import javax.inject.Inject

class SaveGoalUseCase @Inject constructor(
    private val goalDao: GoalDao
) {

    fun validate(
        title: String,
        category: String,
        metricType: String,
        unit: String,
        startValue: Double?,
        targetValue: Double?,
        startDate: Long,
        targetDate: Long
    ): GoalValidationResult {
        var titleError: String? = null
        var dateError: String? = null
        var valueError: String? = null

        if (title.trim().isBlank()) {
            titleError = "Goal title cannot be empty."
        }

        if (category.trim().isBlank()) {
            titleError = titleError ?: "Category cannot be empty."
        }

        if (metricType != "COUNT" && metricType != "HOURS") {
            titleError = titleError ?: "Metric type must be COUNT or HOURS."
        }

        if (unit.trim().isBlank()) {
            titleError = titleError ?: "Metric unit cannot be empty (e.g. 'questions', 'hours')."
        }

        if (startValue == null || startValue < 0.0) {
            valueError = "Starting baseline must be zero or a positive number."
        } else if (targetValue == null || targetValue <= startValue) {
            valueError = "Target value must be greater than starting baseline ($startValue)."
        }

        if (targetDate <= startDate) {
            dateError = "Target deadline must be after the start date."
        }

        val isValid = titleError == null && dateError == null && valueError == null
        return GoalValidationResult(
            isValid = isValid,
            titleError = titleError,
            dateError = dateError,
            valueError = valueError
        )
    }

    suspend operator fun invoke(
        id: Long = 0,
        title: String,
        category: String,
        metricType: String,
        unit: String,
        startValue: Double,
        targetValue: Double,
        startDate: Long,
        targetDate: Long,
        isArchived: Boolean = false
    ): Result<Long> {
        val validation = validate(
            title = title,
            category = category,
            metricType = metricType,
            unit = unit,
            startValue = startValue,
            targetValue = targetValue,
            startDate = startDate,
            targetDate = targetDate
        )

        if (!validation.isValid) {
            val errorMsg = listOfNotNull(validation.titleError, validation.valueError, validation.dateError).joinToString(" ")
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        val now = System.currentTimeMillis()
        val entity = GoalEntity(
            id = id,
            title = title.trim(),
            category = category.trim().uppercase(),
            metricType = metricType.trim().uppercase(),
            unit = unit.trim().lowercase(),
            startValue = startValue,
            targetValue = targetValue,
            startDate = startDate,
            targetDate = targetDate,
            isArchived = isArchived,
            createdAt = if (id == 0L) now else now,
            updatedAt = now
        )

        return try {
            val savedId = goalDao.insert(entity)
            Result.success(if (id == 0L) savedId else id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
