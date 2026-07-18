package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryTotal
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository that wraps [ImportedEntryDao] for importing and querying
 * financial entries.
 */
class ImportRepository(private val dao: ImportedEntryDao) {

    /**
     * Inserts a list of entries, returning only those that were actually
     * inserted (duplicates excluded by the ON CONFLICT IGNORE strategy).
     */
    suspend fun insertEntries(entries: List<ImportedEntry>): List<ImportedEntry> {
        val results = dao.insertAll(entries)
        return entries.filterIndexed { index, _ -> results[index] != -1L }
    }

    /** Returns all entries as a reactive Flow. */
    fun getAllEntries(): Flow<List<ImportedEntry>> = dao.getAllEntries()

    /** Returns only expense entries as a reactive Flow. */
    fun getSpendingEntries(): Flow<List<ImportedEntry>> = dao.getSpendingEntries()

    /** Returns only earnings entries as a reactive Flow. */
    fun getEarningsEntries(): Flow<List<ImportedEntry>> = dao.getEarningsEntries()

    /** Deletes all entries from the database. */
    suspend fun deleteAll() = dao.deleteAll()

    /** Deletes all entries and categories from the database. */
    suspend fun clearAllData(categoryDao: CategoryDao) {
        dao.deleteAll()
        categoryDao.deleteAll()
    }

    /** Returns the total entry count. */
    suspend fun count(): Int = dao.count()

    suspend fun getCategoryTotals(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<CategoryTotal> = dao.getCategoryTotals(isExpense, category, startMillis, endMillis)

    suspend fun getEntriesByDateRange(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<ImportedEntry> = dao.getEntriesByDateRange(isExpense, category, startMillis, endMillis)

    suspend fun getMinDate(): Long? = dao.getMinDate()

    suspend fun getMaxDate(): Long? = dao.getMaxDate()
}
