package com.hhldiniz.praondefoiomeudinheiro.data.local

import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.JsAny
import kotlin.js.Promise

/**
 * `window.praOndeExtractPdfRuns`, defined by `pdf-extract.js` (loaded as a
 * module script in `index.html`). It lazily `import()`s pdf.js from a CDN on
 * first call and returns the pages' positioned text runs as JSON — see that
 * file for why the assembling is not done there.
 */
private fun jsExtractPdfRuns(base64: String): Promise<JsAny> = js(
    "window.praOndeExtractPdfRuns(base64)"
)

/**
 * Whether `pdf-extract.js` actually made it onto the page. Calling a missing
 * bridge would fail with a bare "undefined is not a function", which says
 * nothing about the real problem (the script tag missing from `index.html`, or
 * the file not being served next to it).
 */
private fun isPdfBridgeReady(): Boolean = js("typeof window.praOndeExtractPdfRuns === 'function'")

/**
 * The declared `String` return type is what makes the `js(...)` intrinsic
 * convert the JS string result back to a Kotlin `String` here, the same
 * trick `FilePicker.wasmJs.kt`'s `arrayBufferToBase64` relies on.
 */
private fun jsAnyToKotlinString(value: JsAny): String = js("value")

/** The bridge's wire format; kept here so [PdfTextRun] stays free of it. */
@Serializable
private class RunJson(val text: String, val x: Float, val y: Float, val width: Float)

@Serializable
private class PageJson(val runs: List<RunJson>)

@Serializable
private class DocumentJson(val pages: List<PageJson>)

private val pdfJson = Json { ignoreUnknownKeys = true }

/**
 * Runs the extraction in the browser via pdf.js rather than a native PDF
 * engine (there is none available to Kotlin/Wasm), unlike Android (PDFBox)
 * and iOS (PDFKit). `ByteArray` has no direct JS representation usable from
 * a plain `js(...)` snippet, so bytes cross the boundary as base64 — the
 * same round trip `FilePicker.wasmJs.kt` uses in the other direction for
 * reading a picked `File`'s contents.
 *
 * pdf.js returns positioned runs where the other two platforms return
 * whitespace-padded text; [PdfTextLayout] closes that gap, in common code so
 * the JVM tests reach it.
 */
@OptIn(ExperimentalEncodingApi::class)
actual suspend fun extractPdfText(bytes: ByteArray): String {
    check(isPdfBridgeReady()) {
        "pdf-extract.js is not loaded: the PDF bridge script did not run on this page"
    }
    val base64 = Base64.encode(bytes)
    val result = jsExtractPdfRuns(base64).await<JsAny>()
    val document = pdfJson.decodeFromString<DocumentJson>(jsAnyToKotlinString(result))

    return document.pages.joinToString("\n") { page ->
        PdfTextLayout.reconstruct(page.runs.map { PdfTextRun(it.text, it.x, it.y, it.width) })
    }
}
