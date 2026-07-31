package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.SelectedFilesHolder
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.error_no_csv_in_folder
import com.hhldiniz.praondefoiomeudinheiro.resources.error_no_valid_csv_in_folder
import com.hhldiniz.praondefoiomeudinheiro.resources.error_no_valid_files
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher as platformIoDispatcher

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
    /** An error occurred with a descriptive [message]; resolved by the UI. */
    data class Error(val message: UiText) : LandingUiState()
    /** User confirmed the selection; navigate to Home. */
    data object ProceedToHome : LandingUiState()
}

/**
 * ViewModel for the Landing screen. Handles file/folder validation via
 * [SpreadsheetRepository] and state transitions through the [LandingUiState]
 * sealed class. Picking the files themselves is the platform's job (see
 * `rememberSpreadsheetFilePicker`); this only ever sees [PlatformFile]s.
 */
class LandingViewModel(
    private val repository: SpreadsheetRepository,
    private val ioDispatcher: CoroutineDispatcher = platformIoDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LandingUiState>(LandingUiState.Idle)
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    var validFiles: List<PlatformFile> = emptyList()
        private set

    /** Validates a single file picked by the user and transitions to [LandingUiState.ValidationResult] or [LandingUiState.Error]. */
    fun onFilePicked(file: PlatformFile) {
        viewModelScope.launch {
            _uiState.value = LandingUiState.Loading
            val report = withContext(ioDispatcher) { repository.validateFile(file) }
            if (!report.hasValidFiles) {
                val msg = report.invalidFiles.firstOrNull()?.reason
                    ?: UiText.Localized(Res.string.error_no_valid_files)
                _uiState.value = LandingUiState.Error(msg)
            } else {
                validFiles = report.validFiles.map { it.file }
                _uiState.value = LandingUiState.ValidationResult(report)
            }
        }
    }

    /** Validates all CSV/ODS/PDF files inside the picked folder. */
    fun onFolderPicked(folder: PlatformFolder) {
        viewModelScope.launch {
            _uiState.value = LandingUiState.Loading
            val files = withContext(ioDispatcher) { folder.listSpreadsheetFiles() }
            if (files.isEmpty()) {
                _uiState.value = LandingUiState.Error(UiText.Localized(Res.string.error_no_csv_in_folder))
                return@launch
            }
            val report = withContext(ioDispatcher) { repository.validateFiles(files) }
            if (!report.hasValidFiles) {
                _uiState.value = LandingUiState.Error(UiText.Localized(Res.string.error_no_valid_csv_in_folder))
            } else {
                validFiles = report.validFiles.map { it.file }
                _uiState.value = LandingUiState.ValidationResult(report)
            }
        }
    }

    /** Stores the validated files globally and signals navigation to Home. */
    fun onContinue() {
        SelectedFilesHolder.files = validFiles
        _uiState.value = LandingUiState.ProceedToHome
    }

    /** Proceeds to Home without importing any file. */
    fun onSkip() {
        SelectedFilesHolder.files = emptyList()
        _uiState.value = LandingUiState.ProceedToHome
    }

    /** Resets the ViewModel back to the Idle state. */
    fun onReset() {
        _uiState.value = LandingUiState.Idle
        validFiles = emptyList()
    }
}
