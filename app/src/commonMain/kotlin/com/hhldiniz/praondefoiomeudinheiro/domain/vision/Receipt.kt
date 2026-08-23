package com.hhldiniz.praondefoiomeudinheiro.domain.vision

/**
 * One line of a receipt that priced something: what was bought and what it
 * cost. [quantity] is null when the receipt did not spell one out.
 */
data class ReceiptItem(
    val description: String,
    val amount: Double,
    val quantity: Double? = null,
    /** `0f..1f`, the recognizer's confidence over the words this line was built from. */
    val confidence: Float = 1f,
)

/**
 * A receipt ("nota fiscal") the computer-vision reader believes it found in a
 * photo. Like [DetectedTransaction] it is a *proposal*: the user reviews the
 * merchant, the total and the items before anything is written.
 *
 * [total] is the number the user actually paid; [items] are the individual
 * lines, which the user can import instead of the total when they want the
 * spending broken down.
 */
data class DetectedReceipt(
    /** The establishment's name, empty when the header could not be read. */
    val merchant: String,
    /** The issuer's CNPJ as printed, empty when the receipt carried none. */
    val documentId: String,
    val dateMillis: Long,
    val total: Double,
    val items: List<ReceiptItem>,
    /** The category guessed from the merchant and the items; may be blank. */
    val category: String,
    /** `0f..1f`, combining recognizer confidence with how clearly the total was labelled. */
    val confidence: Float,
    val rawTotal: String,
    val rawDate: String,
    /** True when the total came from a labelled line rather than from the largest value seen. */
    val totalWasLabelled: Boolean,
) {
    /** What the items add up to; compare with [total] to spot a misread line. */
    val itemsTotal: Double get() = items.sumOf { it.amount }

    /** True when the items are complete enough to be imported in place of the total. */
    val itemsMatchTotal: Boolean
        get() = items.isNotEmpty() && kotlin.math.abs(itemsTotal - total) <= 0.02 * maxOf(total, 0.01)
}

/** Everything one receipt-reading run produced, ready to be reviewed. */
data class ReceiptAnalysis(
    val fileName: String,
    val receipt: DetectedReceipt,
)
