package com.hhldiniz.praondefoiomeudinheiro.domain.model

import android.net.Uri

data class FileValidationReport(
    val validFiles: List<ValidSpreadsheetFile>,
    val invalidFiles: List<InvalidSpreadsheetFile>
) {
    val hasValidFiles: Boolean get() = validFiles.isNotEmpty()
    val hasInvalidFiles: Boolean get() = invalidFiles.isNotEmpty()
}

data class ValidSpreadsheetFile(
    val name: String,
    val uri: Uri,
    val headerColumns: List<String>,
    val headerRowIndex: Int = 0
)

data class InvalidSpreadsheetFile(
    val name: String,
    val uri: Uri,
    val reason: String
)
