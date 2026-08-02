package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `window.praOndeRecognizeText`, defined by `ocr-extract.js` (loaded as a
 * module script in `index.html`). It lazily `import()`s Tesseract.js from a
 * CDN on first call and returns the recognized words as a JSON string in
 * normalized page coordinates.
 */
private fun jsRecognizeText(base64: String): Promise<JsAny> = js("window.praOndeRecognizeText(base64)")

/** See `PdfParser.wasmJs.kt`: the declared return type is what converts the JS string back. */
private fun jsAnyToKotlinString(value: JsAny): String = js("value")

/** Same guard as the PDF bridge: a missing script must not surface as "undefined is not a function". */
private fun isOcrBridgeReady(): Boolean = js("typeof window.praOndeRecognizeText === 'function'")

@Serializable
private class OcrWord(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 1f,
)

@Serializable
private class OcrPage(val words: List<OcrWord>)

private val ocrJson = Json { ignoreUnknownKeys = true }

/**
 * Browser text recognition through Tesseract.js, since Kotlin/Wasm has no
 * access to a native OCR engine — the same arrangement (and the same base64
 * round trip, `ByteArray` having no direct JS representation from a plain
 * `js(...)` snippet) the PDF import uses with pdf.js.
 *
 * The JS side already normalizes the boxes into the app's top-left `0f..1f`
 * space, so there is nothing to convert here beyond parsing the JSON.
 */
@OptIn(ExperimentalEncodingApi::class)
actual suspend fun recognizeDocumentText(bytes: ByteArray): RecognizedDocument {
    check(isOcrBridgeReady()) {
        "ocr-extract.js is not loaded: the text-recognition bridge script did not run on this page"
    }
    val result = jsRecognizeText(Base64.encode(bytes)).await<JsAny>()
    val page = ocrJson.decodeFromString<OcrPage>(jsAnyToKotlinString(result))

    return RecognizedDocument(
        page.words.map { word ->
            RecognizedWord(
                text = word.text,
                box = BoundingBox(word.left, word.top, word.right, word.bottom),
                confidence = word.confidence,
            )
        }
    )
}
