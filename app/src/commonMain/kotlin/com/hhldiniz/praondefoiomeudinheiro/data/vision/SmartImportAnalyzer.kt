package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.OdsParser
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.hasImageExtension
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.ReceiptAnalysis
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportAnalysis
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportSource
import com.hhldiniz.praondefoiomeudinheiro.platform.recognizeDocumentText

/** Raised when a file the automatic importer does not read is handed to it (a PDF, say). */
class UnsupportedImportSourceException(val fileName: String) : Exception(
    "The automatic importer does not read $fileName"
)

/** Raised when the receipt reader is handed something that is not an image. */
class ReceiptRequiresImageException(val fileName: String) : Exception(
    "Reading a receipt needs a photo, and $fileName is not one"
)

/**
 * The automatic import path, end to end: bytes in, reviewable transactions
 * out, with no assumption about the file's layout.
 *
 * It reads two quite different things, and which one is which is the user's
 * choice on the way in, not a guess made here:
 *
 * - [analyze] takes a **statement**: many transactions laid out as a table,
 *   whether that is a photo, a screenshot or a spreadsheet. A photo goes
 *   through the platform text recognizer and [DocumentLayoutAnalyzer], a
 *   spreadsheet through the parsers the direct import already uses, and from
 *   there both take exactly the same route through
 *   [TransactionFieldClassifier]. That is what makes the feature
 *   format-independent: the classifier only ever sees rows of strings and
 *   works out the meaning of the columns itself.
 * - [analyzeReceipt] takes a photo of a **receipt** — one purchase, a merchant,
 *   a list of items and a total — and reads it with [ReceiptAnalyzer], which
 *   is a different problem from a table and gets a different reader.
 *
 * PDFs belong to neither: they carry their text as text, so the direct import
 * ([com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository])
 * reads them exactly and remains the right path for them.
 *
 * [recognizeText] is injectable so the pipeline can be unit-tested with a
 * canned page of words instead of a real recognizer.
 */
class SmartImportAnalyzer(
    private val recognizeText: suspend (ByteArray) -> RecognizedDocument = { recognizeDocumentText(it) },
) {

    /** Reads, recognizes and classifies [file]. Failure means the file could not be read at all. */
    suspend fun analyze(file: PlatformFile): Result<SmartImportAnalysis> = runCatching {
        val source = sourceOf(file.name, file.mimeType)
            ?: throw UnsupportedImportSourceException(file.name)
        val bytes = file.readBytes()

        val (rows, baseConfidence) = when (source) {
            SmartImportSource.IMAGE -> {
                val document = recognizeText(bytes)
                val grid = DocumentLayoutAnalyzer.analyze(document)
                grid.rows to grid.averageConfidence
            }
            SmartImportSource.ODS -> OdsParser.parse(bytes) to 1f
            SmartImportSource.CSV -> CsvParser.parse(bytes) to 1f
        }

        val interpretation = TransactionFieldClassifier.classify(rows)
        val transactions = TransactionFieldClassifier.extract(interpretation, baseConfidence)

        SmartImportAnalysis(
            fileName = file.name,
            source = source,
            mappings = interpretation.mappings,
            transactions = transactions,
            rowsScanned = interpretation.dataRows.size,
            confidence = interpretation.confidence,
        )
    }

    /**
     * Reads [file] as a photographed receipt. Only images qualify — a receipt
     * is read from how it was printed, so there is nothing to recognize in a
     * spreadsheet of one. A success with no [ReceiptAnalysis] means the photo
     * was read but carried no amount, i.e. it is probably not a receipt.
     */
    suspend fun analyzeReceipt(file: PlatformFile): Result<ReceiptAnalysis?> = runCatching {
        if (sourceOf(file.name, file.mimeType) != SmartImportSource.IMAGE) {
            throw ReceiptRequiresImageException(file.name)
        }

        val document = recognizeText(file.readBytes())
        ReceiptAnalyzer.analyze(document)?.let { receipt ->
            ReceiptAnalysis(fileName = file.name, receipt = receipt)
        }
    }

    /**
     * Picks the reader for [fileName], falling back to [mimeType] when the
     * name carries no useful extension (the web file picker and some Android
     * providers hand over names like `document`). Null means the automatic
     * importer does not read this kind of file.
     */
    fun sourceOf(fileName: String, mimeType: String? = null): SmartImportSource? = when {
        fileName.hasImageExtension() -> SmartImportSource.IMAGE
        fileName.endsWith(".ods", ignoreCase = true) -> SmartImportSource.ODS
        fileName.endsWith(".pdf", ignoreCase = true) -> null
        fileName.endsWith(".csv", ignoreCase = true) -> SmartImportSource.CSV
        mimeType?.startsWith("image/") == true -> SmartImportSource.IMAGE
        mimeType == "application/pdf" -> null
        mimeType?.contains("spreadsheet") == true -> SmartImportSource.ODS
        else -> SmartImportSource.CSV
    }
}
