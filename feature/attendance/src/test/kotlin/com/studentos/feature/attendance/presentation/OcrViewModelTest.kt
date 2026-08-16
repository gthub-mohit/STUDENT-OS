package com.studentos.feature.attendance.presentation

import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.data.ocr.GridTimetableParser
import com.studentos.feature.attendance.data.ocr.OcrProcessor
import com.studentos.feature.attendance.data.ocr.TimetableFieldMapper
import com.studentos.feature.attendance.data.ocr.TimetableValidator
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import com.studentos.feature.attendance.domain.usecase.ImportTimetableUseCase
import com.studentos.feature.attendance.presentation.viewmodel.OcrUiState
import com.studentos.feature.attendance.presentation.viewmodel.OcrViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrViewModelTest {

    private fun createMapper(): TimetableFieldMapper {
        val validator = TimetableValidator()
        val gridParser = GridTimetableParser(validator)
        return TimetableFieldMapper(gridParser, validator)
    }

    private class FakeTimetableRepository : TimetableRepository {
        var shouldFailImportWithoutReplace = false
        var importedSlots: List<ParsedTimetableSlot>? = null

        override fun getAllSlots(): kotlinx.coroutines.flow.Flow<List<com.studentos.core.database.entity.TimetableSlotEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())

        override suspend fun importTimetable(
            slots: List<ParsedTimetableSlot>,
            replaceExisting: Boolean,
            horizonDays: Int
        ): AppResult<Unit> {
            if (shouldFailImportWithoutReplace && !replaceExisting) {
                return AppResult.Failure(AppError.ValidationError("Timetable already exists"))
            }
            importedSlots = slots
            return AppResult.Success(Unit)
        }

        override suspend fun addSlot(slot: com.studentos.core.database.entity.TimetableSlotEntity, horizonDays: Int): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateSlot(slot: com.studentos.core.database.entity.TimetableSlotEntity, horizonDays: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteSlot(slotId: Long): AppResult<Unit> = AppResult.Success(Unit)
    }

    @Test
    fun initialState_isIdle() {
        val mapper = createMapper()
        val processor = OcrProcessor(mapper)
        val repo = FakeTimetableRepository()
        val useCase = ImportTimetableUseCase(repo)
        val viewModel = OcrViewModel(processor, useCase)

        assertEquals(OcrUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun addSlot_addsSlotToContentState() = runBlocking {
        val mapper = createMapper()
        val processor = OcrProcessor(mapper)
        val repo = FakeTimetableRepository()
        val useCase = ImportTimetableUseCase(repo)
        val viewModel = OcrViewModel(processor, useCase)

        // Seed with a dummy content state by adding a slot
        val newSlot = ParsedTimetableSlot(
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "10:00",
            subjectName = "Software Architecture"
        )

        // Force a Content state for testing slot actions
        val field = OcrViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        (field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OcrUiState>).value = OcrUiState.Content(
            slots = emptyList(),
            hasWarnings = false
        )

        viewModel.addSlot(newSlot)

        val state = viewModel.uiState.first { (it as? OcrUiState.Content)?.slots?.isNotEmpty() == true } as OcrUiState.Content
        assertEquals(1, state.slots.size)
        assertEquals("Software Architecture", state.slots[0].subjectName)
    }

    @Test
    fun updateSlot_updatesSlotAtIndex() = runBlocking {
        val mapper = createMapper()
        val processor = OcrProcessor(mapper)
        val repo = FakeTimetableRepository()
        val useCase = ImportTimetableUseCase(repo)
        val viewModel = OcrViewModel(processor, useCase)

        val field = OcrViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        (field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OcrUiState>).value = OcrUiState.Content(
            slots = listOf(
                ParsedTimetableSlot(dayOfWeek = 1, startTime = "09:00", endTime = "10:00", subjectName = "Math")
            ),
            hasWarnings = false
        )

        viewModel.updateSlot(0, ParsedTimetableSlot(dayOfWeek = 1, startTime = "09:00", endTime = "10:00", subjectName = "Advanced Math"))

        val state = viewModel.uiState.first { (it as? OcrUiState.Content)?.slots?.firstOrNull()?.subjectName == "Advanced Math" } as OcrUiState.Content
        assertEquals("Advanced Math", state.slots[0].subjectName)
    }

    @Test
    fun removeSlot_removesSlotAtIndex() = runBlocking {
        val mapper = createMapper()
        val processor = OcrProcessor(mapper)
        val repo = FakeTimetableRepository()
        val useCase = ImportTimetableUseCase(repo)
        val viewModel = OcrViewModel(processor, useCase)

        val field = OcrViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        (field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OcrUiState>).value = OcrUiState.Content(
            slots = listOf(
                ParsedTimetableSlot(dayOfWeek = 1, startTime = "09:00", endTime = "10:00", subjectName = "Math")
            ),
            hasWarnings = false
        )

        viewModel.removeSlot(0)

        val state = viewModel.uiState.first { (it as? OcrUiState.Content)?.slots?.isEmpty() == true } as OcrUiState.Content
        assertTrue(state.slots.isEmpty())
    }

    @Test
    fun confirmImport_existingTimetable_showsReplaceDialog() = runBlocking {
        val mapper = createMapper()
        val processor = OcrProcessor(mapper)
        val repo = FakeTimetableRepository().apply { shouldFailImportWithoutReplace = true }
        val useCase = ImportTimetableUseCase(repo)
        val viewModel = OcrViewModel(processor, useCase)

        val field = OcrViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        (field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OcrUiState>).value = OcrUiState.Content(
            slots = listOf(
                ParsedTimetableSlot(dayOfWeek = 1, startTime = "09:00", endTime = "10:00", subjectName = "Math")
            ),
            hasWarnings = false
        )

        viewModel.confirmImport(replaceExisting = false)

        val state = viewModel.uiState.first { (it as? OcrUiState.Content)?.showReplaceDialog == true } as OcrUiState.Content
        assertTrue(state.showReplaceDialog)

        viewModel.dismissReplaceDialog()
        val dismissedState = viewModel.uiState.first { (it as? OcrUiState.Content)?.showReplaceDialog == false } as OcrUiState.Content
        assertFalse(dismissedState.showReplaceDialog)
    }

    @Test
    fun confirmImport_success_transitionsToImportSuccess() = runBlocking {
        val mapper = createMapper()
        val processor = OcrProcessor(mapper)
        val repo = FakeTimetableRepository()
        val useCase = ImportTimetableUseCase(repo)
        val viewModel = OcrViewModel(processor, useCase)

        val field = OcrViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        (field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OcrUiState>).value = OcrUiState.Content(
            slots = listOf(
                ParsedTimetableSlot(dayOfWeek = 1, startTime = "09:00", endTime = "10:00", subjectName = "Math")
            ),
            hasWarnings = false
        )

        viewModel.confirmImport(replaceExisting = false)

        val state = viewModel.uiState.first { it is OcrUiState.ImportSuccess }
        assertEquals(OcrUiState.ImportSuccess, state)
    }
}
