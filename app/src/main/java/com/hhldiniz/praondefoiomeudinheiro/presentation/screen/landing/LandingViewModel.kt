package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvUriHolder
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Possible UI states for the Landing screen. */
sealed class LandingUiState {
    /** Initial state waiting for user action. */
    data object Idle : LandingUiState()
    /** Files are being validated. */
    data object Loading : LandingUiState()
    /** Validation completed with the given [report]. */
    data class ValidationResult(
        val report: FileValidationReport,
    ) : LandingUiState()
    /** An error occurred with a descriptive [message]. */
    data class Error(val message: String) : LandingUiState()
    /** User confirmed the selection; navigate to Home. */
    data object ProceedToHome : LandingUiState()
}

/**
 * ViewModel for the Landing screen. Handles file/folder picking, validation
 * via [FileSpreadsheetRepository], and state transitions through the
 * [LandingUiState] sealed class.
 */
class LandingViewModel(
    private val repository: SpreadsheetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LandingUiState>(LandingUiState.Idle)
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    var validUris: List<Uri> = emptyList()
        private set

    /** Validates a single file picked by the user and transitions to [LandingUiState.ValidationResult] or [LandingUiState.Error]. */
    fun onFilePicked(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.value = LandingUiState.Loading
            val report = repository.validateFile(uri, context)
            if (!report.hasValidFiles) {
                val msg = report.invalidFiles.firstOrNull()?.reason
                    ?: context.getString(R.string.error_no_valid_files)
                _uiState.value = LandingUiState.Error(msg)
            } else {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                validUris = report.validFiles.map { it.uri }
                _uiState.value = LandingUiState.ValidationResult(report)
            }
        }
    }

    /** Validates all CSV/ODS files inside the picked folder. */
    fun onFolderPicked(treeUri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.value = LandingUiState.Loading
            val csvUris = withContext(Dispatchers.IO) {
                listCsvUris(context, treeUri)
            }
            if (csvUris.isEmpty()) {
                _uiState.value = LandingUiState.Error(
                    context.getString(R.string.error_no_csv_in_folder)
                )
                return@launch
            }
            val report = repository.validateFiles(csvUris, context)
            if (!report.hasValidFiles) {
                _uiState.value = LandingUiState.Error(
                    context.getString(R.string.error_no_valid_csv_in_folder)
                )
            } else {
                validUris = report.validFiles.map { it.uri }
                _uiState.value = LandingUiState.ValidationResult(report)
            }
        }
    }

    /** Stores the validated URIs globally and signals navigation to Home. */
    fun onContinue() {
        CsvUriHolder.uris = validUris
        _uiState.value = LandingUiState.ProceedToHome
    }

    /** Proceeds to Home without importing any file. */
    fun onSkip() {
        CsvUriHolder.uris = emptyList()
        _uiState.value = LandingUiState.ProceedToHome
    }

    /** Resets the ViewModel back to the Idle state. */
    fun onReset() {
        _uiState.value = LandingUiState.Idle
        validUris = emptyList()
    }

    /** Enumerates CSV/ODS file URIs inside the given document tree. */
    private fun listCsvUris(context: Context, treeUri: Uri): List<Uri> {
        val documentFile = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return documentFile.listFiles()
            .filter { file ->
                val n = file.name
                n?.endsWith(".csv", ignoreCase = true) == true ||
                n?.endsWith(".ods", ignoreCase = true) == true
            }
            .mapNotNull { it.uri }
    }
}
