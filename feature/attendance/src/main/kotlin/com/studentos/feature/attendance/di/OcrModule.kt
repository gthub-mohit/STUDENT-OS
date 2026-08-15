package com.studentos.feature.attendance.di

import com.studentos.feature.attendance.data.ocr.GridTimetableParser
import com.studentos.feature.attendance.data.ocr.OcrProcessor
import com.studentos.feature.attendance.data.ocr.TimetableFieldMapper
import com.studentos.feature.attendance.data.ocr.TimetableValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * OcrModule — Hilt module providing singletons for OCR processing and timetable field mapping.
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideTimetableValidator(): TimetableValidator {
        return TimetableValidator()
    }

    @Provides
    @Singleton
    fun provideGridTimetableParser(validator: TimetableValidator): GridTimetableParser {
        return GridTimetableParser(validator)
    }

    @Provides
    @Singleton
    fun provideTimetableFieldMapper(
        gridParser: GridTimetableParser,
        validator: TimetableValidator
    ): TimetableFieldMapper {
        return TimetableFieldMapper(gridParser, validator)
    }

    @Provides
    @Singleton
    fun provideOcrProcessor(mapper: TimetableFieldMapper): OcrProcessor {
        return OcrProcessor(mapper)
    }
}
