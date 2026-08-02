package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import platform.Foundation.NSMakeRange
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * Apple's Vision framework, part of iOS since 13, so nothing is bundled and
 * nothing is uploaded — like PDFKit on the PDF path, the platform already has
 * the engine this feature needs.
 *
 * Vision hands back one observation per recognized line, in normalized
 * coordinates whose origin is the *bottom* left. Both differences are ironed
 * out here: y is flipped into the top-left space the shared layout analyzer
 * expects, and each line is split into words with
 * `VNRecognizedText.boundingBoxForRange`, so the analyzer can see the gaps
 * between columns instead of one box per line.
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun recognizeDocumentText(bytes: ByteArray): RecognizedDocument {
    val request = VNRecognizeTextRequest()
    request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
    request.usesLanguageCorrection = true
    request.recognitionLanguages = listOf("pt-BR", "en-US", "es-ES")

    val handler = VNImageRequestHandler(data = bytes.toNSData(), options = emptyMap<Any?, Any?>())
    if (!handler.performRequests(listOf(request), null)) {
        throw IllegalArgumentException("Unsupported or corrupt image")
    }

    val words = mutableListOf<RecognizedWord>()
    request.results.orEmpty().filterIsInstance<VNRecognizedTextObservation>().forEach { observation ->
        val candidate = observation.topCandidates(1.convert())
            .filterIsInstance<VNRecognizedText>()
            .firstOrNull() ?: return@forEach
        words += splitIntoWords(candidate, observation.confidence, observation.boundingBoxRect())
    }

    return RecognizedDocument(words)
}

/**
 * Splits one recognized line into words, asking Vision where each of them sits
 * and falling back to a proportional estimate inside [lineBox] when it cannot
 * say (which it does for ranges it did not recognize character-by-character).
 */
@OptIn(ExperimentalForeignApi::class)
private fun splitIntoWords(
    candidate: VNRecognizedText,
    confidence: Float,
    lineBox: BoundingBox,
): List<RecognizedWord> {
    val text = candidate.string
    if (text.isBlank()) return emptyList()

    val words = mutableListOf<RecognizedWord>()
    var index = 0
    while (index < text.length) {
        if (text[index].isWhitespace()) {
            index++
            continue
        }
        var end = index
        while (end < text.length && !text[end].isWhitespace()) end++

        val box = boundingBoxForRange(candidate, index, end - index)
            ?: estimatedBox(lineBox, index, end, text.length)
        words += RecognizedWord(text.substring(index, end), box, confidence)
        index = end
    }
    return words
}

@OptIn(ExperimentalForeignApi::class)
private fun boundingBoxForRange(candidate: VNRecognizedText, location: Int, length: Int): BoundingBox? =
    runCatching {
        candidate
            .boundingBoxForRange(NSMakeRange(location.convert(), length.convert()), null)
            ?.boundingBoxRect()
    }.getOrNull()

/** Spreads a word across [lineBox] by character position; a last resort only. */
private fun estimatedBox(lineBox: BoundingBox, start: Int, end: Int, length: Int): BoundingBox {
    val safeLength = length.coerceAtLeast(1).toFloat()
    return BoundingBox(
        left = lineBox.left + lineBox.width * (start / safeLength),
        top = lineBox.top,
        right = lineBox.left + lineBox.width * (end / safeLength),
        bottom = lineBox.bottom,
    )
}

/**
 * Converts a Vision `boundingBox` (normalized, origin bottom-left, y growing
 * upwards) into the app's normalized top-left space.
 */
@OptIn(ExperimentalForeignApi::class)
private fun platform.Vision.VNDetectedObjectObservation.boundingBoxRect(): BoundingBox =
    boundingBox.useContents {
        BoundingBox(
            left = origin.x.toFloat(),
            top = (1.0 - (origin.y + size.height)).toFloat(),
            right = (origin.x + size.width).toFloat(),
            bottom = (1.0 - origin.y).toFloat(),
        )
    }
