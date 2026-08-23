package com.hhldiniz.praondefoiomeudinheiro.domain.vision

/** A role a column of an imported table can play. */
enum class TransactionField {
    DATE,
    AMOUNT,
    DESCRIPTION,
    CATEGORY,

    /**
     * The credit side of a statement that splits money out and money in into
     * two columns ("Débito | Crédito"), detected next to [AMOUNT].
     */
    CREDIT_AMOUNT,

    /** A column spelling the direction out in words ("débito", "entrada", "income"). */
    TYPE,
}

/**
 * Where the rows handed to the classifier came from.
 *
 * PDFs are deliberately absent: a PDF already carries its text as text, so it
 * belongs to the direct import, which reads it exactly instead of guessing at
 * a reconstructed layout. The automatic path takes what has no usable
 * structure of its own — a photo, a screenshot, a loosely shaped table.
 */
enum class SmartImportSource {
    /** A photo or screenshot, read by the platform text recognizer. */
    IMAGE,
    CSV,
    ODS,
}

/** One column the classifier assigned to a [field], with how sure it is. */
data class FieldMapping(
    val field: TransactionField,
    val columnIndex: Int,
    /** The header cell backing this mapping, empty when the table had no header row. */
    val header: String,
    val confidence: Float,
)

/**
 * A transaction the automatic importer believes it found. It is a *proposal*:
 * the user reviews (and can drop or re-type) each one before anything is
 * written to the database.
 */
data class DetectedTransaction(
    val dateMillis: Long,
    val amount: Double,
    val description: String,
    val category: String,
    val isExpense: Boolean,
    /** `0f..1f`, combining recognizer confidence with how cleanly the row parsed. */
    val confidence: Float,
    val rawDate: String,
    val rawAmount: String,
)

/** Everything one automatic-import run produced, ready to be reviewed. */
data class SmartImportAnalysis(
    val fileName: String,
    val source: SmartImportSource,
    val mappings: List<FieldMapping>,
    val transactions: List<DetectedTransaction>,
    /** Rows the classifier considered data rows, whether or not they yielded a transaction. */
    val rowsScanned: Int,
    /** `0f..1f` for the column mapping as a whole; low values are worth warning about. */
    val confidence: Float,
) {
    val isEmpty: Boolean get() = transactions.isEmpty()

    fun columnOf(field: TransactionField): Int? =
        mappings.firstOrNull { it.field == field }?.columnIndex
}
