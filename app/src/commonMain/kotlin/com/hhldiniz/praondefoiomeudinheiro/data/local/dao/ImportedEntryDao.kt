package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val total: Double,
)

/**
 * [ImportedEntry] persistence contract. Room-backed on Android/iOS
 * (`RoomImportedEntryDao` in `roomMain`), localStorage-backed on wasmJs
 * (`WebImportedEntryDao`).
 */
interface ImportedEntryDao {

    /** Inserts a batch of entries, ignoring conflicts on the unique index. Returns row IDs. */
    suspend fun insertAll(entries: List<ImportedEntry>): List<Long>

    /** Returns all entries ordered by date descending, as a reactive Flow. */
    fun getAllEntries(): Flow<List<ImportedEntry>>

    /** Returns only expense entries ordered by date descending. */
    fun getSpendingEntries(): Flow<List<ImportedEntry>>

    /** Returns only earnings entries ordered by date descending. */
    fun getEarningsEntries(): Flow<List<ImportedEntry>>

    /** Returns all entries as a plain (non-reactive) list ordered by date descending. */
    suspend fun getAllEntriesByDate(): List<ImportedEntry>

    /** Deletes every row from the table. */
    suspend fun deleteAll()

    /** Returns the total number of stored entries. */
    suspend fun count(): Int

    /** Category totals for pie chart, optionally filtered by date range and category. */
    suspend fun getCategoryTotals(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<CategoryTotal>

    /** Raw entries within a date range for line chart aggregation. */
    suspend fun getEntriesByDateRange(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<ImportedEntry>

    suspend fun getMinDate(): Long?

    suspend fun getMaxDate(): Long?

    /** Distinct category names across all entries, for filter dropdowns. Cheap compared to aggregating totals. */
    suspend fun getDistinctCategories(): List<String>

    /** One page of entries (both spending and earnings) within a date range, for the entries list. */
    suspend fun getEntriesPage(
        category: String?,
        startMillis: Long,
        endMillis: Long,
        limit: Int,
        offset: Int,
    ): List<ImportedEntry>
}
