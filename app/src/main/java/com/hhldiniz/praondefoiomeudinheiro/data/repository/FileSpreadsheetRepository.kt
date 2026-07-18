package com.hhldiniz.praondefoiomeudinheiro.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.OdsParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.SpreadsheetFileValidator
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValueRange
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository

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

    override suspend fun readValues(uri: Uri, contentResolver: ContentResolver): Result<ValueRange> {
        return runCatching {
            val fileName = uri.lastPathSegment ?: ""
            val isOds = fileName.endsWith(".ods", ignoreCase = true)
            val rows = contentResolver.openInputStream(uri)?.use { stream ->
                if (isOds) OdsParser.parse(stream) else CsvParser.parse(stream)
            } ?: throw IllegalStateException("Cannot open file")

            val headerRowIndex = findHeaderRowIndex(rows)
            val dataRows = if (headerRowIndex >= 0) {
                rows.drop(headerRowIndex + 1).filter { row ->
                    row.any { it.isNotBlank() }
                }
            } else {
                rows.drop(1)
            }

            val spendingEntries = dataRows.mapNotNull { row ->
                if (row.size > 4) {
                    CsvEntry(
                        date = row[1].trim(),
                        amount = row[2].trim(),
                        description = row[3].trim(),
                        category = row[4].trim(),
                    )
                } else null
            }

            val earningsEntries = if (dataRows.any { it.size > 9 }) {
                dataRows.mapNotNull { row ->
                    if (row.size > 9) {
                        CsvEntry(
                            date = row[6].trim(),
                            amount = row[7].trim(),
                            description = row[8].trim(),
                            category = row[9].trim(),
                        )
                    } else null
                }
            } else {
                emptyList()
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

    private fun findHeaderRowIndex(rows: List<List<String>>): Int {
        return rows.indexOfFirst { row ->
            row.size >= 10 &&
            row[1].trim().equals("Data", ignoreCase = true) &&
            row[2].trim().equals("Valor", ignoreCase = true) &&
            row[3].trim().equals("Descrição", ignoreCase = true) &&
            row[4].trim().equals("Categoria", ignoreCase = true) &&
            row[6].trim().equals("Data", ignoreCase = true) &&
            row[7].trim().equals("Valor", ignoreCase = true) &&
            row[8].trim().equals("Descrição", ignoreCase = true) &&
            row[9].trim().equals("Categoria", ignoreCase = true)
        }
    }
}
