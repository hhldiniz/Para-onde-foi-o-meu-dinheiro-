package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile

object SpreadsheetFileValidator {

    private val EXPECTED_HEADERS = listOf("Data", "Valor", "Descrição", "Categoria")
    private const val DESPESA_COL_START = 1
    private const val RENDA_COL_START = 6
    private const val NUM_COLUMNS = 4

    fun validate(
        uri: Uri,
        context: Context
    ): Pair<ValidSpreadsheetFile?, InvalidSpreadsheetFile?> {
        val contentResolver = context.contentResolver
        val fileName = resolveFileName(uri, contentResolver, context)

        if (!hasSupportedExtension(uri, fileName, contentResolver)) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_unsupported_format)
            )
        }

        val isOds = fileName.endsWith(".ods", ignoreCase = true)
        val rows = try {
            contentResolver.openInputStream(uri)?.use { stream ->
                if (isOds) OdsParser.parse(stream) else CsvParser.parse(stream)
            }
        } catch (e: Exception) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_cannot_read_file, e.message)
            )
        }

        if (rows == null || rows.isEmpty()) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_empty_file)
            )
        }

        if (rows.size < 2) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_no_header_or_data)
            )
        }

        val isStructured = hasStructuredMarkers(rows)

        return if (isStructured) {
            validateStructured(rows, fileName, uri, context)
        } else {
            validateSimple(rows, fileName, uri)
        }
    }

    private fun hasStructuredMarkers(rows: List<List<String>>): Boolean {
        return rows.any { row ->
            row.any { it.trim().equals("Despesas", ignoreCase = true) }
        } || rows.any { row ->
            row.any { it.trim().equals("Renda", ignoreCase = true) }
        }
    }

    private fun validateStructured(
        rows: List<List<String>>,
        fileName: String,
        uri: Uri,
        context: Context
    ): Pair<ValidSpreadsheetFile?, InvalidSpreadsheetFile?> {
        val headerIndex = findHeaderRowIndex(rows)

        if (headerIndex < 0) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_header_not_found)
            )
        }

        val headerRow = rows[headerIndex]

        val hasDespesasHeaders = checkHeaders(headerRow, DESPESA_COL_START)
        val hasRendaHeaders = checkHeaders(headerRow, RENDA_COL_START)

        if (!hasDespesasHeaders && !hasRendaHeaders) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_invalid_header)
            )
        }

        val dataRows = rows.drop(headerIndex + 1).filter { row ->
            row.any { it.isNotBlank() }
        }

        if (dataRows.isEmpty()) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_no_data_rows)
            )
        }

        val displayHeaders = mutableListOf<String>()
        if (hasDespesasHeaders) displayHeaders.add(context.getString(R.string.header_expenses))
        if (hasRendaHeaders) displayHeaders.add(context.getString(R.string.header_income))

        val valid = ValidSpreadsheetFile(
            name = fileName,
            uri = uri,
            headerColumns = displayHeaders,
            headerRowIndex = headerIndex
        )

        return valid to null
    }

    private fun findHeaderRowIndex(rows: List<List<String>>): Int {
        return rows.indexOfFirst { row ->
            row.size >= RENDA_COL_START + NUM_COLUMNS &&
            checkHeaders(row, DESPESA_COL_START) &&
            checkHeaders(row, RENDA_COL_START)
        }
    }

    private fun checkHeaders(row: List<String>, startIndex: Int): Boolean {
        if (startIndex + NUM_COLUMNS > row.size) return false
        return EXPECTED_HEADERS.indices.all { i ->
            row[startIndex + i].trim().equals(EXPECTED_HEADERS[i], ignoreCase = true)
        }
    }

    private fun validateSimple(
        rows: List<List<String>>,
        fileName: String,
        uri: Uri
    ): Pair<ValidSpreadsheetFile?, InvalidSpreadsheetFile?> {
        val header = rows.first().map { it.trim() }.filter { it.isNotBlank() }
        val valid = ValidSpreadsheetFile(
            name = fileName,
            uri = uri,
            headerColumns = header,
            headerRowIndex = 0
        )

        return valid to null
    }

    private fun hasSupportedExtension(
        uri: Uri,
        fileName: String,
        contentResolver: android.content.ContentResolver
    ): Boolean {
        if (fileName.endsWith(".csv", ignoreCase = true)) return true
        if (fileName.endsWith(".ods", ignoreCase = true)) return true
        val type = contentResolver.getType(uri)
        return type == "text/csv" || type == "text/comma-separated-values"
    }

    private fun resolveFileName(uri: Uri, contentResolver: android.content.ContentResolver, context: Context): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) it.getString(nameIndex) else ""
        } ?: uri.lastPathSegment ?: context.getString(R.string.unknown_file)
    }
}
