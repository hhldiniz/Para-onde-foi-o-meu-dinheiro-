package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument

/**
 * Runs on-device text recognition over an encoded image (PNG/JPEG/…) and
 * returns every word it found with its position on the page.
 *
 * Each platform brings its own engine — ML Kit on Android, the Vision
 * framework on iOS, Tesseract.js (loaded lazily from a CDN) on wasmJs — but
 * they all normalize their output into the same top-left `0f..1f` coordinate
 * space, so [com.hhldiniz.praondefoiomeudinheiro.data.vision.DocumentLayoutAnalyzer]
 * and everything above it stay platform-agnostic. Nothing is uploaded
 * anywhere: recognition runs on the device, like the rest of this app.
 *
 * Implementations throw when the bytes are not a decodable image; callers
 * (see [com.hhldiniz.praondefoiomeudinheiro.data.vision.SmartImportAnalyzer])
 * turn that into a user-facing message.
 */
expect suspend fun recognizeDocumentText(bytes: ByteArray): RecognizedDocument
