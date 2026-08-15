package com.studentos.feature.attendance.data.ocr.model

/**
 * Geometric bounding rectangle for OCR elements.
 *
 * @param left Left coordinate (x-min)
 * @param top Top coordinate (y-min)
 * @param right Right coordinate (x-max)
 * @param bottom Bottom coordinate (y-max)
 */
data class OcrRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2

    fun intersects(other: OcrRect): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }

    fun contains(x: Int, y: Int): Boolean {
        return x in left..right && y in top..bottom
    }

    companion object {
        val ZERO = OcrRect(0, 0, 0, 0)
    }
}

/**
 * Represents an individual recognized text block or line from OCR with bounding box coordinates.
 *
 * @param text The string content
 * @param rect The bounding box
 * @param confidence Confidence score between 0.0f and 1.0f
 */
data class OcrTextElement(
    val text: String,
    val rect: OcrRect,
    val confidence: Float = 1.0f
)
