package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.domain.repository.FocusAssignmentRepository
import com.studentos.feature.intelligence.domain.repository.FocusAttendanceRepository
import com.studentos.feature.intelligence.domain.repository.FocusDsaRepository
import com.studentos.feature.intelligence.domain.repository.FocusProjectRepository
import javax.inject.Inject

/**
 * ToggleFocusItemUseCase — Orchestrates focus-item completion by delegating to the appropriate
 * feature-specific domain repository contract.
 *
 * Preserves validation, domain invariants, and AppEvent broadcasting for each feature.
 */
class ToggleFocusItemUseCase @Inject constructor(
    private val assignmentRepository: FocusAssignmentRepository,
    private val attendanceRepository: FocusAttendanceRepository,
    private val dsaRepository: FocusDsaRepository,
    private val projectRepository: FocusProjectRepository
) {
    suspend operator fun invoke(item: TodayFocusItem) {
        val entityId = item.entityId ?: return
        val newCompleted = !item.isCompleted
        when (item.category.uppercase()) {
            "ASSIGNMENT" -> assignmentRepository.setAssignmentCompleted(entityId, newCompleted)
            "ATTENDANCE" -> attendanceRepository.setAttendanceStatus(entityId, newCompleted)
            "DSA" -> dsaRepository.setTopicRevised(entityId, newCompleted)
            "PROJECT" -> projectRepository.setProjectTaskCompleted(entityId, newCompleted)
        }
    }
}
