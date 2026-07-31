package com.hhldiniz.praondefoiomeudinheiro.data.local

/**
 * Parser for PDF files exported in the app's supported spreadsheet layout.
 * Extracts text in reading order and splits each line into columns on runs
 * of two or more whitespace characters (the column padding produced by
 * table/report generators), returning the same `List<List<String>>` row
 * shape as [CsvParser] and [OdsParser].
 *
 * Only the text extraction itself is platform-specific ([extractPdfText]);
 * PDFBox on Android, PDFKit on iOS.
 */
object PdfParser {

    private val COLUMN_SEPARATOR = Regex("\\s{2,}")

    /** Parses the PDF, returning one row per non-blank line, split into columns. */
    fun parse(bytes: ByteArray): List<List<String>> = splitIntoRows(extractPdfText(bytes))

    /** Splits already-extracted PDF [text] into rows/columns; shared by both platforms. */
    fun splitIntoRows(text: String): List<List<String>> =
        text.lineSequence()
            .filter { it.isNotBlank() }
            .map { line -> line.trim().split(COLUMN_SEPARATOR).map { it.trim() } }
            .toList()
}

/** Extracts all text from a PDF document in reading order. */
expect fun extractPdfText(bytes: ByteArray): String
