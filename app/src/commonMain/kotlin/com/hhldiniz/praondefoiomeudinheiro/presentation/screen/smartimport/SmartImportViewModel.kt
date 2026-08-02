package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.vision.SmartImportAnalyzer
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_error_failed
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_error_no_transactions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher as platformIoDispatcher

/**
 * Drives the automatic import: hand it a picked file, it runs
 * [SmartImportAnalyzer] off the main thread and exposes the proposed
 * transactions for review.
 *
 * Nothing is written until [confirmImport]; the analysis stage is deliberately
 * read-only so a misread statement costs the user a tap, not a cleanup.
 */
class SmartImportViewModel(
    private val importRepository: ImportRepository,
    private val categoryRepository: CategoryRepository,
    private val analyzer: SmartImportAnalyzer,
    private val ioDispatcher: CoroutineDispatcher = platformIoDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartImportUiState())
    val uiState: StateFlow<SmartImportUiState> = _uiState.asStateFlow()

    /** Analyzes [file] and moves to the review stage (or reports why it could not). */
    fun onFilePicked(file: PlatformFile) {
        _uiState.value = SmartImportUiState(
            stage = SmartImportStage.ANALYZING,
            fileName = file.name,
        )

        viewModelScope.launch {
            val result = withContext(ioDispatcher) { analyzer.analyze(file) }

            result.onSuccess { analysis ->
                if (analysis.isEmpty) {
                    _uiState.update {
                        it.copy(
                            stage = SmartImportStage.IDLE,
                            source = analysis.source,
                            error = UiText.Localized(Res.string.smart_import_error_no_transactions),
                        )
                    }
                    return@onSuccess
                }
                _uiState.update {
                    it.copy(
                        stage = SmartImportStage.REVIEW,
                        source = analysis.source,
                        mappings = analysis.mappings,
                        candidates = analysis.transactions.map { transaction ->
                            TransactionCandidate(transaction)
                        },
                        rowsScanned = analysis.rowsScanned,
                        confidence = analysis.confidence,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        stage = SmartImportStage.IDLE,
                        error = UiText.Localized(
                            Res.string.smart_import_error_failed,
                            listOf(error.message ?: ""),
                        ),
                    )
                }
            }
        }
    }

    /** Keeps or drops the candidate at [index]. */
    fun onCandidateToggled(index: Int) {
        updateCandidate(index) { it.copy(selected = !it.selected) }
    }

    /** Flips a candidate between expense and income, for the rows the classifier called wrong. */
    fun onCandidateTypeToggled(index: Int) {
        updateCandidate(index) { it.copy(isExpense = !it.isExpense) }
    }

    private fun updateCandidate(index: Int, transform: (TransactionCandidate) -> TransactionCandidate) {
        _uiState.update { state ->
            if (index !in state.candidates.indices) return@update state
            state.copy(
                candidates = state.candidates.mapIndexed { position, candidate ->
                    if (position == index) transform(candidate) else candidate
                }
            )
        }
    }

    /** Writes the selected candidates, reusing the importer's duplicate handling. */
    fun confirmImport() {
        val state = _uiState.value
        val selected = state.candidates.filter { it.selected }
        if (selected.isEmpty() || state.stage == SmartImportStage.SAVING) return

        _uiState.update { it.copy(stage = SmartImportStage.SAVING) }

        viewModelScope.launch {
            val entries = selected.map { candidate ->
                ImportedEntry(
                    dateMillis = candidate.transaction.dateMillis,
                    amount = candidate.transaction.amount,
                    description = candidate.transaction.description,
                    category = candidate.transaction.category,
                    isExpense = candidate.isExpense,
                    fileName = state.fileName,
                )
            }

            val (inserted, newCategories) = withContext(ioDispatcher) {
                val insertedEntries = importRepository.insertEntries(entries)
                val existing = categoryRepository.getAllSync().map { it.name }.toSet()
                val categories = insertedEntries
                    .map { it.category }
                    .filter { it.isNotBlank() && it !in existing }
                    .distinct()
                if (categories.isNotEmpty()) categoryRepository.insertAll(categories)
                insertedEntries to categories.size
            }

            if (inserted.isNotEmpty()) DataClearedHolder.reset()

            _uiState.update {
                it.copy(
                    stage = SmartImportStage.DONE,
                    importedCount = inserted.size,
                    duplicateCount = entries.size - inserted.size,
                    newCategoryCount = newCategories,
                )
            }
        }
    }

    /** Clears the current analysis so another file can be picked. */
    fun reset() {
        _uiState.value = SmartImportUiState()
    }
}
