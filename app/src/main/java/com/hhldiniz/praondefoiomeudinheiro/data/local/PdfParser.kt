package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

/**
 * Parser for PDF files exported in the app's supported spreadsheet layout.
 * Extracts text in reading order and splits each line into columns on runs
 * of two or more whitespace characters (the column padding produced by
 * table/report generators), returning the same `List<List<String>>` row
 * shape as [CsvParser] and [OdsParser].
 */
object PdfParser {

    private val COLUMN_SEPARATOR = Regex("\\s{2,}")

    @Volatile
    private var initialized = false

    /**
     * Loads PDFBox's font/glyph resources from the APK's assets. Required
     * once before any [parse] call — PDFBox-Android cannot read its bundled
     * resources via the plain JVM classloader lookup that desktop PDFBox
     * relies on. Called from `PraondefoiomeudinheiroApp.onCreate`.
     */
    fun init(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    /** Parses the PDF, returning one row per non-blank line, split into columns. */
    fun parse(inputStream: InputStream): List<List<String>> {
        return PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            val text = stripper.getText(document)
            text.lineSequence()
                .filter { it.isNotBlank() }
                .map { line -> line.trim().split(COLUMN_SEPARATOR).map { it.trim() } }
                .toList()
        }
    }
}
