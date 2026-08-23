package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.vision.ReceiptRequiresImageException
import com.hhldiniz.praondefoiomeudinheiro.data.vision.SmartImportAnalyzer
import com.hhldiniz.praondefoiomeudinheiro.data.vision.UnsupportedImportSourceException
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedReceipt
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedTransaction
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_error_failed
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_error_no_receipt
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_error_no_transactions
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_error_receipt_needs_image
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_error_unsupported
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
 * Two readings share this flow. [onFilePicked] reads a statement — a table of
 * many transactions — and [onReceiptPicked] reads a photographed receipt into
 * one purchase, which [onItemizedToggled] can then split into its items. Both
 * end up as the same list of [TransactionCandidate]s, so the review and the
 * writing below them are shared.
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

    /** Analyzes [file] as a statement and moves to review (or reports why it could not). */
    fun onFilePicked(file: PlatformFile) {
        _uiState.value = SmartImportUiState(
            stage = SmartImportStage.ANALYZING,
            mode = SmartImportMode.STATEMENT,
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
            }.onFailure { error -> reportFailure(error) }
        }
    }

    /**
     * Reads [file] as a photographed receipt. The total is proposed as a
     * single expense; [onItemizedToggled] swaps it for the individual items.
     */
    fun onReceiptPicked(file: PlatformFile) {
        _uiState.value = SmartImportUiState(
            stage = SmartImportStage.ANALYZING,
            mode = SmartImportMode.RECEIPT,
            fileName = file.name,
        )

        viewModelScope.launch {
            val result = withContext(ioDispatcher) { analyzer.analyzeReceipt(file) }

            result.onSuccess { analysis ->
                val receipt = analysis?.receipt
                if (receipt == null) {
                    _uiState.update {
                        it.copy(
                            stage = SmartImportStage.IDLE,
                            error = UiText.Localized(Res.string.smart_import_error_no_receipt),
                        )
                    }
                    return@onSuccess
                }
                _uiState.update {
                    it.copy(
                        stage = SmartImportStage.REVIEW,
                        receipt = receipt,
                        itemized = false,
                        candidates = candidatesFor(receipt, itemized = false),
                        rowsScanned = receipt.items.size,
                        confidence = receipt.confidence,
                        error = null,
                    )
                }
            }.onFailure { error -> reportFailure(error) }
        }
    }

    /**
     * Switches between importing the receipt's total as one entry and
     * importing each of its items, rebuilding the candidates either way.
     */
    fun onItemizedToggled() {
        val state = _uiState.value
        val receipt = state.receipt ?: return
        if (receipt.items.isEmpty() || state.stage != SmartImportStage.REVIEW) return

        val itemized = !state.itemized
        _uiState.update {
            it.copy(itemized = itemized, candidates = candidatesFor(receipt, itemized))
        }
    }

    /**
     * A receipt as reviewable candidates: its total as one expense, or one per
     * item. Everything carries the receipt's date, category and confidence,
     * since that is all the receipt said about them.
     */
    private fun candidatesFor(receipt: DetectedReceipt, itemized: Boolean): List<TransactionCandidate> {
        if (!itemized || receipt.items.isEmpty()) {
            return listOf(
                TransactionCandidate(
                    DetectedTransaction(
                        dateMillis = receipt.dateMillis,
                        amount = receipt.total,
                        description = receipt.merchant,
                        category = receipt.category,
                        isExpense = true,
                        confidence = receipt.confidence,
                        rawDate = receipt.rawDate,
                        rawAmount = receipt.rawTotal,
                    )
                )
            )
        }

        return receipt.items.map { item ->
            TransactionCandidate(
                DetectedTransaction(
                    dateMillis = receipt.dateMillis,
                    amount = item.amount,
                    description = item.description,
                    category = receipt.category,
                    isExpense = true,
                    confidence = minOf(receipt.confidence, item.confidence),
                    rawDate = receipt.rawDate,
                    rawAmount = item.amount.toString(),
                )
            )
        }
    }

    /** Turns a failed analysis into the most specific message that fits it. */
    private fun reportFailure(error: Throwable) {
        val message = when (error) {
            is UnsupportedImportSourceException ->
                UiText.Localized(Res.string.smart_import_error_unsupported)

            is ReceiptRequiresImageException ->
                UiText.Localized(Res.string.smart_import_error_receipt_needs_image)

            else -> UiText.Localized(
                Res.string.smart_import_error_failed,
                listOf(error.message ?: ""),
            )
        }
        _uiState.update { it.copy(stage = SmartImportStage.IDLE, error = message) }
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
