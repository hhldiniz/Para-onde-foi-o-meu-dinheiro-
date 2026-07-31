package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.OdsParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.PdfParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.SpreadsheetFileValidator
import com.hhldiniz.praondefoiomeudinheiro.data.local.TransactionColumnMapper
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValueRange
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository

/** Implementation of [SpreadsheetRepository] using local file parsers and validators. */
class FileSpreadsheetRepository : SpreadsheetRepository {

    override suspend fun validateFile(file: PlatformFile): FileValidationReport {
        val (valid, invalid) = SpreadsheetFileValidator.validate(file)

        val validFiles = if (valid != null) listOf(valid) else emptyList()
        val invalidFiles = if (invalid != null) listOf(invalid) else emptyList()

        return FileValidationReport(
            validFiles = validFiles,
            invalidFiles = invalidFiles
        )
    }

    /** Validates each file in [files] and aggregates the results. */
    override suspend fun validateFiles(files: List<PlatformFile>): FileValidationReport {
        val valid = mutableListOf<ValidSpreadsheetFile>()
        val invalid = mutableListOf<InvalidSpreadsheetFile>()

        for (file in files) {
            val (v, i) = SpreadsheetFileValidator.validate(file)
            if (v != null) valid.add(v)
            if (i != null) invalid.add(i)
        }

        return FileValidationReport(
            validFiles = valid,
            invalidFiles = invalid
        )
    }

    /**
     * Reads cell values from the given file, auto-detecting ODS/PDF/CSV from
     * the file name extension. The header row is scanned by
     * [TransactionColumnMapper] to locate date/amount/description/category
     * columns regardless of language or column order; each recognized table
     * within that row becomes a spending or earnings group by position (see
     * [TransactionColumnMapper.ColumnGroup]).
     */
    override suspend fun readValues(file: PlatformFile): Result<ValueRange> {
        return runCatching {
            val fileName = file.name
            val bytes = file.readBytes()
            val rows = when {
                fileName.endsWith(".ods", ignoreCase = true) -> OdsParser.parse(bytes)
                fileName.endsWith(".pdf", ignoreCase = true) -> PdfParser.parse(bytes)
                else -> CsvParser.parse(bytes)
            }

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

            ValueRange(
                range = "$fileName!A1:Z${dataRows.size}",
                rows = dataRows,
                spendingEntries = spendingEntries,
                earningsEntries = earningsEntries,
            )
        }
    }
}
