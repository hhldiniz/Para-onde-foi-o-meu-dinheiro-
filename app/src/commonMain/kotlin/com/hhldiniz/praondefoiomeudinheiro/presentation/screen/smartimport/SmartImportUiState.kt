package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedTransaction
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.FieldMapping
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportSource

/** Where the automatic import flow currently is. */
enum class SmartImportStage {
    /** Nothing picked yet. */
    IDLE,

    /** Reading, recognizing and classifying the picked file. */
    ANALYZING,

    /** Showing what was found, waiting for the user to confirm. */
    REVIEW,

    SAVING,
    DONE,
}

/**
 * One proposed transaction plus the decisions the user can still make about
 * it before it is written: whether to keep it, and whether it is an expense.
 */
data class TransactionCandidate(
    val transaction: DetectedTransaction,
    val selected: Boolean = true,
    val isExpense: Boolean = transaction.isExpense,
)

data class SmartImportUiState(
    val stage: SmartImportStage = SmartImportStage.IDLE,
    val fileName: String = "",
    val source: SmartImportSource? = null,
    val mappings: List<FieldMapping> = emptyList(),
    val candidates: List<TransactionCandidate> = emptyList(),
    val rowsScanned: Int = 0,
    val confidence: Float = 0f,
    val error: UiText? = null,
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
    val newCategoryCount: Int = 0,
) {
    val selectedCount: Int get() = candidates.count { it.selected }

    /** Below this the mapping is worth a warning, not a refusal — the user decides. */
    val isLowConfidence: Boolean get() = confidence < 0.6f
}
