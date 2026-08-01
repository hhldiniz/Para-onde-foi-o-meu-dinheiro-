package com.hhldiniz.praondefoiomeudinheiro.data.local

/**
 * Parser for PDF files exported in the app's supported spreadsheet layout.
 * Extracts text in reading order and splits each line into columns on runs
 * of two or more whitespace characters (the column padding produced by
 * table/report generators), returning the same `List<List<String>>` row
 * shape as [CsvParser] and [OdsParser].
 *
 * Only the text extraction itself is platform-specific ([extractPdfText]);
 * PDFBox on Android, PDFKit on iOS, pdf.js (loaded lazily from a CDN) on
 * wasmJs. It is `suspend` because the wasmJs actual has to await a JS
 * Promise; the Android/iOS actuals stay synchronous under the hood.
 */
object PdfParser {

    private val COLUMN_SEPARATOR = Regex("\\s{2,}")

    /** Parses the PDF, returning one row per non-blank line, split into columns. */
    suspend fun parse(bytes: ByteArray): List<List<String>> = splitIntoRows(extractPdfText(bytes))

    /** Splits already-extracted PDF [text] into rows/columns; shared by all platforms. */
    fun splitIntoRows(text: String): List<List<String>> =
        text.lineSequence()
            .filter { it.isNotBlank() }
            .map { line -> line.trim().split(COLUMN_SEPARATOR).map { it.trim() } }
            .toList()
}

/**
 * Extracts all text from a PDF document in reading order, columns padded
 * with whitespace where the source layout had visual gaps between them (see
 * [PdfParser.splitIntoRows]'s `\s{2,}` column splitter, which every actual's
 * output must remain compatible with).
 */
expect suspend fun extractPdfText(bytes: ByteArray): String
