package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.OdsParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.PdfParser
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.hasImageExtension
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportAnalysis
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportSource
import com.hhldiniz.praondefoiomeudinheiro.platform.recognizeDocumentText

/**
 * The automatic import path, end to end: bytes in, reviewable transactions
 * out, with no assumption about the file's layout.
 *
 * Whatever the user picked is first turned into a plain `List<List<String>>`
 * table — a photo or screenshot goes through the platform text recognizer and
 * [DocumentLayoutAnalyzer], a spreadsheet/PDF through the parsers the direct
 * import already uses — and from there both take exactly the same route
 * through [TransactionFieldClassifier]. That is what makes the feature
 * format-independent: the classifier only ever sees rows of strings and works
 * out the meaning of the columns itself.
 *
 * The direct import ([com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository])
 * is untouched and remains the faster, exact path for files that already
 * follow the expected format.
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
        val bytes = file.readBytes()

        val (rows, baseConfidence) = when (source) {
            SmartImportSource.IMAGE -> {
                val document = recognizeText(bytes)
                val grid = DocumentLayoutAnalyzer.analyze(document)
                grid.rows to grid.averageConfidence
            }
            SmartImportSource.ODS -> OdsParser.parse(bytes) to 1f
            SmartImportSource.PDF -> PdfParser.parse(bytes) to 1f
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
     * Picks the reader for [fileName], falling back to [mimeType] when the
     * name carries no useful extension (the web file picker and some Android
     * providers hand over names like `document`).
     */
    fun sourceOf(fileName: String, mimeType: String? = null): SmartImportSource = when {
        fileName.hasImageExtension() -> SmartImportSource.IMAGE
        fileName.endsWith(".ods", ignoreCase = true) -> SmartImportSource.ODS
        fileName.endsWith(".pdf", ignoreCase = true) -> SmartImportSource.PDF
        fileName.endsWith(".csv", ignoreCase = true) -> SmartImportSource.CSV
        mimeType?.startsWith("image/") == true -> SmartImportSource.IMAGE
        mimeType == "application/pdf" -> SmartImportSource.PDF
        mimeType?.contains("spreadsheet") == true -> SmartImportSource.ODS
        else -> SmartImportSource.CSV
    }
}
