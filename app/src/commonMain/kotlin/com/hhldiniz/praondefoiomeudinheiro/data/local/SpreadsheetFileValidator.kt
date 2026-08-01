package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.error_cannot_read_file
import com.hhldiniz.praondefoiomeudinheiro.resources.error_empty_file
import com.hhldiniz.praondefoiomeudinheiro.resources.error_header_not_found
import com.hhldiniz.praondefoiomeudinheiro.resources.error_no_data_rows
import com.hhldiniz.praondefoiomeudinheiro.resources.error_no_header_or_data
import com.hhldiniz.praondefoiomeudinheiro.resources.error_unsupported_format
import org.jetbrains.compose.resources.StringResource

/**
 * Validates spreadsheet files (CSV/ODS/PDF) by checking extension support,
 * file readability, row structure, and the presence of a recognizable
 * transaction table — see [TransactionColumnMapper] for how columns are
 * matched regardless of language, order, or how many side-by-side tables
 * the header row contains.
 *
 * Failures are reported as [UiText] rather than resolved strings, so the
 * validator stays free of the resource loader and the message is rendered in
 * whatever language the UI is showing.
 */
object SpreadsheetFileValidator {

    /**
     * Validates [file]. Returns a pair where at most one side is non-null:
     * a [ValidSpreadsheetFile] on success or an [InvalidSpreadsheetFile]
     * describing the failure.
     */
    suspend fun validate(
        file: PlatformFile,
    ): Pair<ValidSpreadsheetFile?, InvalidSpreadsheetFile?> {
        val fileName = file.name

        if (!hasSupportedExtension(fileName, file.mimeType)) {
            return file.rejected(Res.string.error_unsupported_format)
        }

        val rows = try {
            parseByExtension(fileName, file.readBytes())
        } catch (e: Exception) {
            return file.rejected(Res.string.error_cannot_read_file, e.message ?: "")
        }

        if (rows.isEmpty()) {
            return file.rejected(Res.string.error_empty_file)
        }

        if (rows.size < 2) {
            return file.rejected(Res.string.error_no_header_or_data)
        }

        val headerIndex = TransactionColumnMapper.findHeaderRowIndex(rows)
        if (headerIndex < 0) {
            return file.rejected(Res.string.error_header_not_found)
        }

        val dataRows = rows.drop(headerIndex + 1).filter { row -> row.any { it.isNotBlank() } }
        if (dataRows.isEmpty()) {
            return file.rejected(Res.string.error_no_data_rows)
        }

        val headerColumns = rows[headerIndex].map { it.trim() }.filter { it.isNotBlank() }
        val valid = ValidSpreadsheetFile(
            name = fileName,
            file = file,
            headerColumns = headerColumns,
            headerRowIndex = headerIndex
        )

        return valid to null
    }

    private fun PlatformFile.rejected(
        message: StringResource,
        vararg args: String,
    ): Pair<ValidSpreadsheetFile?, InvalidSpreadsheetFile?> =
        null to InvalidSpreadsheetFile(
            name = name,
            file = this,
            reason = UiText.Localized(message, args.toList()),
        )

    /** Checks whether the file has a supported extension (.csv / .ods / .pdf) or MIME type. */
    private fun hasSupportedExtension(fileName: String, mimeType: String?): Boolean {
        if (fileName.endsWith(".csv", ignoreCase = true)) return true
        if (fileName.endsWith(".ods", ignoreCase = true)) return true
        if (fileName.endsWith(".pdf", ignoreCase = true)) return true
        return mimeType == "text/csv" ||
            mimeType == "text/comma-separated-values" ||
            mimeType == "application/pdf"
    }

    /** Picks the parser matching [fileName]'s extension (.ods / .pdf / else CSV). */
    private suspend fun parseByExtension(fileName: String, bytes: ByteArray): List<List<String>> {
        return when {
            fileName.endsWith(".ods", ignoreCase = true) -> OdsParser.parse(bytes)
            fileName.endsWith(".pdf", ignoreCase = true) -> PdfParser.parse(bytes)
            else -> CsvParser.parse(bytes)
        }
    }
}
