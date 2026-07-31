package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.platform.toNSData
import platform.PDFKit.PDFDocument

/**
 * PDFKit is part of iOS, so the PDF import path needs no third-party library
 * here (Android uses PDFBox). `PDFDocument.string` already returns the text in
 * reading order, which is what [PdfParser] expects.
 */
actual fun extractPdfText(bytes: ByteArray): String =
    PDFDocument(data = bytes.toNSData())?.string() ?: ""
