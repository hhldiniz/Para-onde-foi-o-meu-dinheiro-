package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvUriHolder
import com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LandingUiState {
    data object Idle : LandingUiState()
    data object Loading : LandingUiState()
    data class ValidationResult(
        val report: FileValidationReport,
    ) : LandingUiState()
    data class Error(val message: String) : LandingUiState()
    data object ProceedToHome : LandingUiState()
}

class LandingViewModel : ViewModel() {

    private val repository = FileSpreadsheetRepository()
    private val _uiState = MutableStateFlow<LandingUiState>(LandingUiState.Idle)
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    var validUris: List<Uri> = emptyList()
        private set

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

    fun onContinue() {
        CsvUriHolder.uris = validUris
        _uiState.value = LandingUiState.ProceedToHome
    }

    fun onReset() {
        _uiState.value = LandingUiState.Idle
        validUris = emptyList()
    }

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
