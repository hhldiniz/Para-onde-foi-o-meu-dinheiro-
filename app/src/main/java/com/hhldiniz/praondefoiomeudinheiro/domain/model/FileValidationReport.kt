package com.hhldiniz.praondefoiomeudinheiro.domain.model

import android.net.Uri

/**
 * Result of validating one or more spreadsheet files. Carries separate lists
 * for files that passed validation and those that failed, along with
 * convenience flags.
 */
data class FileValidationReport(
    val validFiles: List<ValidSpreadsheetFile>,
    val invalidFiles: List<InvalidSpreadsheetFile>
) {
    val hasValidFiles: Boolean get() = validFiles.isNotEmpty()
    val hasInvalidFiles: Boolean get() = invalidFiles.isNotEmpty()
}

/**
 * Describes a spreadsheet file that passed validation, including its display
 * name, content URI, detected header columns, and the row index where the
 * header was found.
 */
data class ValidSpreadsheetFile(
    val name: String,
    val uri: Uri,
    val headerColumns: List<String>,
    val headerRowIndex: Int = 0
)

/**
 * Describes a spreadsheet file that failed validation, including the reason
 * for the failure (e.g. unsupported format, empty file, missing headers).
 */
data class InvalidSpreadsheetFile(
    val name: String,
    val uri: Uri,
    val reason: String
)
