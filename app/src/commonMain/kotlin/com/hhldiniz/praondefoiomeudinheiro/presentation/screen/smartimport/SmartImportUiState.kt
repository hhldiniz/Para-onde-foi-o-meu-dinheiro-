package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedReceipt
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedTransaction
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.FieldMapping
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportSource

/** Which of the two automatic readings the user asked for. */
enum class SmartImportMode {
    /** Many transactions laid out as a table: a photo, a screenshot or a spreadsheet. */
    STATEMENT,

    /** One purchase printed as a receipt, read from a photo by computer vision. */
    RECEIPT,
}

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
    val mode: SmartImportMode = SmartImportMode.STATEMENT,
    val fileName: String = "",
    val source: SmartImportSource? = null,
    /** The receipt that was read, on the [SmartImportMode.RECEIPT] path only. */
    val receipt: DetectedReceipt? = null,
    /** True while the receipt's items, rather than its total, are up for import. */
    val itemized: Boolean = false,
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

    /** True when the receipt listed items, so the user can choose to split it. */
    val canItemize: Boolean get() = receipt?.items?.isNotEmpty() == true

    /** Below this the mapping is worth a warning, not a refusal — the user decides. */
    val isLowConfidence: Boolean get() = confidence < 0.6f
}
