package com.studentos.feature.attendance.data.ocr

import android.graphics.Bitmap
import android.util.Log
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
            logDebug("ML Kit extracted ${visionText.textBlocks.size} text blocks")
            val result = mapper.map(visionText)
            logDebug("Mapped to ${result.slots.size} slots with hasWarnings=${result.hasWarnings}")
            result
        } catch (e: Exception) {
            logDebug("OCR Processing Error: ${e.message}")
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

    private fun logDebug(message: String) {
        try {
            Log.d("TimetableOcr", message)
        } catch (_: Exception) {}
    }
}
