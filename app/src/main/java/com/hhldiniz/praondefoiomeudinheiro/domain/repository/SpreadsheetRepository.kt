package com.hhldiniz.praondefoiomeudinheiro.domain.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValueRange

/**
 * Repository contract for spreadsheet file operations: validation and data reading.
 */
interface SpreadsheetRepository {

    /** Validates a single spreadsheet file, returning a report with valid/invalid results. */
    suspend fun validateFile(uri: Uri, context: Context): FileValidationReport

    /** Validates a batch of spreadsheet files, aggregating all results into a single report. */
    suspend fun validateFiles(uris: List<Uri>, context: Context): FileValidationReport

    /** Reads the cell values from a spreadsheet file and parses them into a [ValueRange]. */
    suspend fun readValues(uri: Uri, contentResolver: ContentResolver): Result<ValueRange>
}
