package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.platform.toNSData
import platform.PDFKit.PDFDocument

/**
 * PDFKit is part of iOS, so the PDF import path needs no third-party library
 * here (Android uses PDFBox). `PDFDocument.string` already returns the text in
 * reading order, which is what [PdfParser] expects.
 */
// `suspend` only to satisfy the common `expect` (wasmJs genuinely awaits a JS
// Promise); PDFKit itself runs synchronously here, same as before.
actual suspend fun extractPdfText(bytes: ByteArray): String {
    // `initWithData:` returns nil for anything PDFKit cannot open.
    val document: PDFDocument? = PDFDocument(data = bytes.toNSData())
    return document?.string ?: ""
}
