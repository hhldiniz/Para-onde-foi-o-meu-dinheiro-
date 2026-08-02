package com.hhldiniz.praondefoiomeudinheiro.data.local

import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.JsAny
import kotlin.js.Promise

/**
 * `window.praOndeExtractPdfText`, defined by `pdf-extract.js` (loaded as a
 * module script in `index.html`). It lazily `import()`s pdf.js from a CDN on
 * first call, extracts text per page, and reconstructs whitespace-padded
 * columns from pdf.js's raw glyph-position data — see that file for why.
 */
private fun jsExtractPdfText(base64: String): Promise<JsAny> = js(
    "window.praOndeExtractPdfText(base64)"
)

/**
 * Whether `pdf-extract.js` actually made it onto the page. Calling a missing
 * bridge would fail with a bare "undefined is not a function", which says
 * nothing about the real problem (the script tag missing from `index.html`, or
 * the file not being served next to it).
 */
private fun isPdfBridgeReady(): Boolean = js("typeof window.praOndeExtractPdfText === 'function'")

/**
 * The declared `String` return type is what makes the `js(...)` intrinsic
 * convert the JS string result back to a Kotlin `String` here, the same
 * trick `FilePicker.wasmJs.kt`'s `arrayBufferToBase64` relies on.
 */
private fun jsAnyToKotlinString(value: JsAny): String = js("value")

/**
 * Runs the extraction in the browser via pdf.js rather than a native PDF
 * engine (there is none available to Kotlin/Wasm), unlike Android (PDFBox)
 * and iOS (PDFKit). `ByteArray` has no direct JS representation usable from
 * a plain `js(...)` snippet, so bytes cross the boundary as base64 — the
 * same round trip `FilePicker.wasmJs.kt` uses in the other direction for
 * reading a picked `File`'s contents.
 */
@OptIn(ExperimentalEncodingApi::class)
actual suspend fun extractPdfText(bytes: ByteArray): String {
    check(isPdfBridgeReady()) {
        "pdf-extract.js is not loaded: the PDF bridge script did not run on this page"
    }
    val base64 = Base64.encode(bytes)
    val result = jsExtractPdfText(base64).await<JsAny>()
    return jsAnyToKotlinString(result)
}
