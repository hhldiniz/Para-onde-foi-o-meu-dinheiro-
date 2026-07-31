package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import kotlinx.serialization.Serializable

/**
 * A single financial entry imported from a spreadsheet. Room-free so it can
 * be shared with wasmJs; the Room-backed persistence for Android/iOS lives in
 * `roomMain` as `ImportedEntryRecord`, which mirrors this shape 1:1 (unique
 * index on date/amount/description/category/is_expense prevents duplicate
 * imports there).
 */
@Serializable
data class ImportedEntry(
    val id: Long = 0,
    val dateMillis: Long,
    val amount: Double,
    val description: String,
    val category: String,
    val isExpense: Boolean,
    val fileName: String = "",
    val importedAt: Long = currentTimeMillis(),
)
