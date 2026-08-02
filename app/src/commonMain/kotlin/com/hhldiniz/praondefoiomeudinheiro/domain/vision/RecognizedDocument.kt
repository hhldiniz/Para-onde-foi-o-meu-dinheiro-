package com.hhldiniz.praondefoiomeudinheiro.domain.vision

/**
 * An axis-aligned rectangle in *normalized page coordinates*: `0f..1f` on both
 * axes, measured from the top-left corner with `y` growing downwards.
 *
 * Every platform text recognizer normalizes into this space before handing
 * words to the shared layout analyzer, because each of them speaks something
 * different natively — ML Kit reports pixels from the top-left, Vision reports
 * fractions from the *bottom*-left, Tesseract.js reports pixels. Normalizing at
 * the edge keeps [com.hhldiniz.praondefoiomeudinheiro.data.vision.DocumentLayoutAnalyzer]
 * free of per-platform quirks and makes its thresholds resolution-independent.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    /** The smallest box containing both this one and [other]. */
    fun union(other: BoundingBox) = BoundingBox(
        left = minOf(left, other.left),
        top = minOf(top, other.top),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom),
    )
}

/**
 * One recognized token (usually a word) together with where it sits on the
 * page and how sure the recognizer is about it, in `0f..1f`. Recognizers that
 * report no per-word confidence use `1f`.
 */
data class RecognizedWord(
    val text: String,
    val box: BoundingBox,
    val confidence: Float = 1f,
)

/**
 * The full output of running text recognition over one page/image: the words
 * in no particular order (the layout analyzer sorts them geometrically).
 */
data class RecognizedDocument(
    val words: List<RecognizedWord>,
) {
    val isEmpty: Boolean get() = words.isEmpty()

    /** Mean per-word confidence, used as a floor for per-row confidence later. */
    val averageConfidence: Float
        get() = if (words.isEmpty()) 0f else words.map { it.confidence }.average().toFloat()
}
