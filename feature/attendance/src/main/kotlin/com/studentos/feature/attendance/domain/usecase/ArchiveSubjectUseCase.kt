package com.studentos.feature.attendance.domain.usecase

import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * ArchiveSubjectUseCase — Domain use case for archiving a subject.
 *
 * Validates that no active timetable slots exist for the subject before archiving.
 */
class ArchiveSubjectUseCase @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val timetableSlotDao: TimetableSlotDao
) {

    suspend operator fun invoke(
        subjectId: Long,
        confirmWithActiveSlots: Boolean = false
    ): AppResult<Unit> {
        if (!confirmWithActiveSlots) {
            val activeSlots = timetableSlotDao.getSlotsForSubject(subjectId).first()
            if (activeSlots.isNotEmpty()) {
                return AppResult.Failure(
                    AppError.ValidationError("Active timetable slots exist for this subject.")
                )
            }
        }
        return subjectRepository.archiveSubject(subjectId)
    }
}
