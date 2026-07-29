package com.studentos.feature.attendance.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.studentos.feature.attendance.domain.model.OcrResult
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * OcrProcessor — Wraps ML Kit Text Recognition engine and maps extracted text to [OcrResult].
 */
class OcrProcessor @Inject constructor(
    private val mapper: TimetableFieldMapper
) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Processes an input [Bitmap] image using ML Kit OCR and returns the parsed [OcrResult].
     */
    suspend fun extract(bitmap: Bitmap): OcrResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = processImage(inputImage)
            mapper.map(visionText)
        } catch (e: Exception) {
            OcrResult(
                slots = emptyList(),
                hasWarnings = true,
                rawText = "OCR Processing Error: ${e.message ?: "Unknown error"}"
            )
        }
    }

    private suspend fun processImage(image: InputImage): Text = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(visionText)
                }
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(exception))
                }
            }
    }
}
