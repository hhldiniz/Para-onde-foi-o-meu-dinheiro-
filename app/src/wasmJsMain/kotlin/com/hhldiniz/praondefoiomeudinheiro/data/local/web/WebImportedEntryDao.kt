package com.hhldiniz.praondefoiomeudinheiro.data.local.web

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryTotal
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val STORAGE_KEY = "praondefoiomeudinheiro.imported_entries"

/**
 * [ImportedEntryDao] backed by `localStorage`, replacing Room on wasmJs
 * (Room 2.7.1 has no wasmJs target; see the `roomMain` source set). The
 * whole entry list is kept in memory and re-serialized to `localStorage` on
 * every mutation, which is fine at the scale of an imported spreadsheet.
 * Filtering/sorting/paging is re-implemented in plain Kotlin to match the
 * SQL queries [com.hhldiniz.praondefoiomeudinheiro.data.local.dao.RoomImportedEntryDao] runs.
 */
class WebImportedEntryDao : ImportedEntryDao {

    private val state = MutableStateFlow(load())

    private fun load(): List<ImportedEntry> {
        val raw = localStorage.getItem(STORAGE_KEY) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<ImportedEntry>>(raw) }.getOrDefault(emptyList())
    }

    private fun persist(entries: List<ImportedEntry>) {
        localStorage.setItem(STORAGE_KEY, Json.encodeToString(entries))
    }

    private fun ImportedEntry.dedupeKey() = listOf(dateMillis, amount, description, category, isExpense)

    /**
     * Mirrors Room's `OnConflictStrategy.IGNORE` insert against the unique
     * index on (date, amount, description, category, is_expense), including
     * conflicts between two rows of the same incoming batch (Room inserts
     * sequentially, so an earlier row in the batch can shadow a later one).
     */
    override suspend fun insertAll(entries: List<ImportedEntry>): List<Long> {
        val current = state.value.toMutableList()
        val existingKeys = current.mapTo(mutableSetOf()) { it.dedupeKey() }
        var nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1
        val results = ArrayList<Long>(entries.size)
        for (entry in entries) {
            if (!existingKeys.add(entry.dedupeKey())) {
                results += -1L
            } else {
                val id = nextId++
                current += entry.copy(id = id)
                results += id
            }
        }
        state.value = current
        persist(current)
        return results
    }

    override fun getAllEntries(): Flow<List<ImportedEntry>> =
        state.map { it.sortedByDescending(ImportedEntry::dateMillis) }

    override fun getSpendingEntries(): Flow<List<ImportedEntry>> =
        state.map { entries -> entries.filter(ImportedEntry::isExpense).sortedByDescending(ImportedEntry::dateMillis) }

    override fun getEarningsEntries(): Flow<List<ImportedEntry>> =
        state.map { entries -> entries.filterNot(ImportedEntry::isExpense).sortedByDescending(ImportedEntry::dateMillis) }

    override suspend fun getAllEntriesByDate(): List<ImportedEntry> =
        state.value.sortedByDescending(ImportedEntry::dateMillis)

    override suspend fun deleteAll() {
        state.value = emptyList()
        persist(emptyList())
    }

    override suspend fun count(): Int = state.value.size

    override suspend fun getCategoryTotals(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<CategoryTotal> = state.value
        .asSequence()
        .filter { it.isExpense == isExpense }
        .filter { category == null || it.category == category }
        .filter { it.dateMillis in startMillis..endMillis }
        .groupBy { it.category }
        .map { (categoryName, entries) -> CategoryTotal(categoryName, entries.sumOf { it.amount }) }
        .sortedByDescending { it.total }

    override suspend fun getEntriesByDateRange(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<ImportedEntry> = state.value
        .asSequence()
        .filter { it.isExpense == isExpense }
        .filter { category == null || it.category == category }
        .filter { it.dateMillis in startMillis..endMillis }
        .sortedBy { it.dateMillis }
        .toList()

    override suspend fun getMinDate(): Long? = state.value.minOfOrNull { it.dateMillis }

    override suspend fun getMaxDate(): Long? = state.value.maxOfOrNull { it.dateMillis }

    override suspend fun getDistinctCategories(): List<String> = state.value
        .asSequence()
        .map { it.category }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()

    override suspend fun getEntriesPage(
        category: String?,
        startMillis: Long,
        endMillis: Long,
        limit: Int,
        offset: Int,
    ): List<ImportedEntry> = state.value
        .asSequence()
        .filter { category == null || it.category == category }
        .filter { it.dateMillis in startMillis..endMillis }
        .sortedByDescending { it.dateMillis }
        .drop(offset)
        .take(limit)
        .toList()
}
