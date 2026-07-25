package com.hhldiniz.praondefoiomeudinheiro.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.OdsParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.PdfParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.SpreadsheetFileValidator
import com.hhldiniz.praondefoiomeudinheiro.data.local.TransactionColumnMapper
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValueRange
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository

/** Implementation of [SpreadsheetRepository] using local file parsers and validators. */
class FileSpreadsheetRepository : SpreadsheetRepository {

    override suspend fun validateFile(uri: Uri, context: Context): FileValidationReport {
        val (valid, invalid) = SpreadsheetFileValidator.validate(uri, context)

        val validFiles = if (valid != null) listOf(valid) else emptyList()
        val invalidFiles = if (invalid != null) listOf(invalid) else emptyList()

        return FileValidationReport(
            validFiles = validFiles,
            invalidFiles = invalidFiles
        )
    }

    /** Validates each URI in [uris] and aggregates the results. */
    override suspend fun validateFiles(uris: List<Uri>, context: Context): FileValidationReport {
        val valid = mutableListOf<ValidSpreadsheetFile>()
        val invalid = mutableListOf<InvalidSpreadsheetFile>()

        for (uri in uris) {
            val (v, i) = SpreadsheetFileValidator.validate(uri, context)
            if (v != null) valid.add(v)
            if (i != null) invalid.add(i)
        }

        return FileValidationReport(
            validFiles = valid,
            invalidFiles = invalid
        )
    }

    /**
     * Reads cell values from the given URI, auto-detecting ODS/PDF/CSV from
     * the file name extension. The header row is scanned by
     * [TransactionColumnMapper] to locate date/amount/description/category
     * columns regardless of language or column order; each recognized table
     * within that row becomes a spending or earnings group by position (see
     * [TransactionColumnMapper.ColumnGroup]).
     */
    override suspend fun readValues(uri: Uri, contentResolver: ContentResolver): Result<ValueRange> {
        return runCatching {
            val fileName = uri.lastPathSegment ?: ""
            val rows = contentResolver.openInputStream(uri)?.use { stream ->
                when {
                    fileName.endsWith(".ods", ignoreCase = true) -> OdsParser.parse(stream)
                    fileName.endsWith(".pdf", ignoreCase = true) -> PdfParser.parse(stream)
                    else -> CsvParser.parse(stream)
                }
            } ?: throw IllegalStateException("Cannot open file")

            val headerRowIndex = TransactionColumnMapper.findHeaderRowIndex(rows)
            val dataRows = if (headerRowIndex >= 0) {
                rows.drop(headerRowIndex + 1).filter { row ->
                    row.any { it.isNotBlank() }
                }
            } else {
                rows.drop(1)
            }

            val groups = if (headerRowIndex >= 0) {
                TransactionColumnMapper.findColumnGroups(rows[headerRowIndex])
            } else {
                // No recognizable header anywhere: fall back to the canonical
                // date/amount/description/category column order, all treated
                // as spending, so a plain unlabeled table still imports.
                listOf(TransactionColumnMapper.ColumnGroup(0, 1, 2, 3, isExpense = true))
            }

            val spendingEntries = mutableListOf<CsvEntry>()
            val earningsEntries = mutableListOf<CsvEntry>()
            for (row in dataRows) {
                for (group in groups) {
                    val entry = TransactionColumnMapper.extractEntry(row, group) ?: continue
                    if (group.isExpense) spendingEntries.add(entry) else earningsEntries.add(entry)
                }
            }

            val range = uri.lastPathSegment?.let { "$it!A1:Z${dataRows.size}" } ?: "A1:Z${dataRows.size}"
            ValueRange(
                range = range,
                rows = dataRows,
                spendingEntries = spendingEntries,
                earningsEntries = earningsEntries,
            )
        }
    }
}
