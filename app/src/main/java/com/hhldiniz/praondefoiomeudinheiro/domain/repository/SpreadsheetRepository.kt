package com.hhldiniz.praondefoiomeudinheiro.domain.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValueRange

interface SpreadsheetRepository {

    suspend fun validateFile(uri: Uri, context: Context): FileValidationReport

    suspend fun validateFiles(uris: List<Uri>, context: Context): FileValidationReport

    suspend fun readValues(uri: Uri, contentResolver: ContentResolver): Result<ValueRange>
}
