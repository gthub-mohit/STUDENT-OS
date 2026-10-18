package com.studentos.feature.intelligence.domain.usecase

import com.studentos.core.database.dao.GoalDao
import javax.inject.Inject

class ManageGoalsUseCase @Inject constructor(
    private val goalDao: GoalDao
) {

    suspend fun setArchived(goalId: Long, isArchived: Boolean): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            goalDao.setArchived(id = goalId, isArchived = isArchived, updatedAt = now)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: Long): Result<Unit> {
        return try {
            goalDao.deleteById(goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
