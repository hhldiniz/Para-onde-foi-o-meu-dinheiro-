package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val total: Double,
)

/** Room DAO for [ImportedEntry] persistence. */
@Dao
interface ImportedEntryDao {

    /** Inserts a batch of entries, ignoring conflicts on the unique index. Returns row IDs. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<ImportedEntry>): List<Long>

    /** Returns all entries ordered by date descending, as a reactive Flow. */
    @Query("SELECT * FROM imported_entries ORDER BY date_millis DESC")
    fun getAllEntries(): Flow<List<ImportedEntry>>

    /** Returns only expense entries ordered by date descending. */
    @Query("SELECT * FROM imported_entries WHERE is_expense = 1 ORDER BY date_millis DESC")
    fun getSpendingEntries(): Flow<List<ImportedEntry>>

    /** Returns only earnings entries ordered by date descending. */
    @Query("SELECT * FROM imported_entries WHERE is_expense = 0 ORDER BY date_millis DESC")
    fun getEarningsEntries(): Flow<List<ImportedEntry>>

    /** Returns all entries as a plain (non-reactive) list ordered by date descending. */
    @Query("SELECT * FROM imported_entries ORDER BY date_millis DESC")
    suspend fun getAllEntriesByDate(): List<ImportedEntry>

    /** Deletes every row from the table. */
    @Query("DELETE FROM imported_entries")
    suspend fun deleteAll()

    /** Returns the total number of stored entries. */
    @Query("SELECT COUNT(*) FROM imported_entries")
    suspend fun count(): Int

    /** Category totals for pie chart, optionally filtered by date range and category. */
    @Query("""
        SELECT category, SUM(amount) AS total FROM imported_entries
        WHERE is_expense = :isExpense
        AND (:category IS NULL OR category = :category)
        AND date_millis BETWEEN :startMillis AND :endMillis
        GROUP BY category ORDER BY total DESC
    """)
    suspend fun getCategoryTotals(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<CategoryTotal>

    /** Raw entries within a date range for line chart aggregation. */
    @Query("""
        SELECT * FROM imported_entries
        WHERE is_expense = :isExpense
        AND (:category IS NULL OR category = :category)
        AND date_millis BETWEEN :startMillis AND :endMillis
        ORDER BY date_millis ASC
    """)
    suspend fun getEntriesByDateRange(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<ImportedEntry>

    @Query("SELECT MIN(date_millis) FROM imported_entries")
    suspend fun getMinDate(): Long?

    @Query("SELECT MAX(date_millis) FROM imported_entries")
    suspend fun getMaxDate(): Long?
}
