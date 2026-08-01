package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Loads PDFBox's font/glyph resources from the APK's assets. Required once
 * before any [extractPdfText] call — PDFBox-Android cannot read its bundled
 * resources via the plain JVM classloader lookup that desktop PDFBox relies
 * on. Called from `PraondefoiomeudinheiroApp.onCreate`.
 */
object PdfBoxInitializer {

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }
}

// `suspend` only to satisfy the common `expect` (wasmJs genuinely awaits a JS
// Promise); PDFBox itself runs synchronously here, same as before.
actual suspend fun extractPdfText(bytes: ByteArray): String =
    PDDocument.load(bytes.inputStream()).use { document ->
        PDFTextStripper().apply { sortByPosition = true }.getText(document)
    }
