package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
    // Json skips a property whose value equals its default, and this default
    // is re-evaluated at encode time: an entry serialized in the same
    // millisecond it was built would be written without its timestamp and
    // read back with whatever "now" was at load time (see the identical fix
    // on Investment.updatedAt). Encoding it always is what makes the stored
    // value survive a reload on wasmJs.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @OptIn(ExperimentalSerializationApi::class)
    val importedAt: Long = currentTimeMillis(),
)
