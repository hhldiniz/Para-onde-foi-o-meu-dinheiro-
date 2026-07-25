package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile

/**
 * Validates spreadsheet files (CSV/ODS/PDF) by checking extension support,
 * file readability, row structure, and the presence of a recognizable
 * transaction table — see [TransactionColumnMapper] for how columns are
 * matched regardless of language, order, or how many side-by-side tables
 * the header row contains.
 */
object SpreadsheetFileValidator {

    /**
     * Validates the file at [uri]. Returns a pair where at most one side is
     * non-null: a [ValidSpreadsheetFile] on success or an [InvalidSpreadsheetFile]
     * describing the failure.
     */
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

        val rows = try {
            contentResolver.openInputStream(uri)?.use { stream ->
                parseByExtension(fileName, stream)
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

        val headerIndex = TransactionColumnMapper.findHeaderRowIndex(rows)
        if (headerIndex < 0) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_header_not_found)
            )
        }

        val dataRows = rows.drop(headerIndex + 1).filter { row -> row.any { it.isNotBlank() } }
        if (dataRows.isEmpty()) {
            return null to InvalidSpreadsheetFile(
                name = fileName,
                uri = uri,
                reason = context.getString(R.string.error_no_data_rows)
            )
        }

        val headerColumns = rows[headerIndex].map { it.trim() }.filter { it.isNotBlank() }
        val valid = ValidSpreadsheetFile(
            name = fileName,
            uri = uri,
            headerColumns = headerColumns,
            headerRowIndex = headerIndex
        )

        return valid to null
    }

    /** Checks whether the file has a supported extension (.csv / .ods / .pdf) or MIME type. */
    private fun hasSupportedExtension(
        uri: Uri,
        fileName: String,
        contentResolver: android.content.ContentResolver
    ): Boolean {
        if (fileName.endsWith(".csv", ignoreCase = true)) return true
        if (fileName.endsWith(".ods", ignoreCase = true)) return true
        if (fileName.endsWith(".pdf", ignoreCase = true)) return true
        val type = contentResolver.getType(uri)
        return type == "text/csv" || type == "text/comma-separated-values" || type == "application/pdf"
    }

    /** Picks the parser matching [fileName]'s extension (.ods / .pdf / else CSV). */
    private fun parseByExtension(fileName: String, stream: java.io.InputStream): List<List<String>> {
        return when {
            fileName.endsWith(".ods", ignoreCase = true) -> OdsParser.parse(stream)
            fileName.endsWith(".pdf", ignoreCase = true) -> PdfParser.parse(stream)
            else -> CsvParser.parse(stream)
        }
    }

    /** Resolves the display file name from a content URI using [OpenableColumns.DISPLAY_NAME]. */
    private fun resolveFileName(uri: Uri, contentResolver: android.content.ContentResolver, context: Context): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) it.getString(nameIndex) else ""
        } ?: uri.lastPathSegment ?: context.getString(R.string.unknown_file)
    }
}
