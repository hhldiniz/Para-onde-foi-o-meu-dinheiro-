package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntryRecord
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.toDomain
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.toRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room DAO for [ImportedEntryRecord] persistence; implements the Room-free [ImportedEntryDao] contract. */
@Dao
interface RoomImportedEntryDao : ImportedEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllRecords(entries: List<ImportedEntryRecord>): List<Long>

    override suspend fun insertAll(entries: List<ImportedEntry>): List<Long> =
        insertAllRecords(entries.map { it.toRecord() })

    @Query("SELECT * FROM imported_entries ORDER BY date_millis DESC")
    fun getAllEntriesRecords(): Flow<List<ImportedEntryRecord>>

    override fun getAllEntries(): Flow<List<ImportedEntry>> =
        getAllEntriesRecords().map { list -> list.map { it.toDomain() } }

    @Query("SELECT * FROM imported_entries WHERE is_expense = 1 ORDER BY date_millis DESC")
    fun getSpendingEntriesRecords(): Flow<List<ImportedEntryRecord>>

    override fun getSpendingEntries(): Flow<List<ImportedEntry>> =
        getSpendingEntriesRecords().map { list -> list.map { it.toDomain() } }

    @Query("SELECT * FROM imported_entries WHERE is_expense = 0 ORDER BY date_millis DESC")
    fun getEarningsEntriesRecords(): Flow<List<ImportedEntryRecord>>

    override fun getEarningsEntries(): Flow<List<ImportedEntry>> =
        getEarningsEntriesRecords().map { list -> list.map { it.toDomain() } }

    @Query("SELECT * FROM imported_entries ORDER BY date_millis DESC")
    suspend fun getAllEntriesByDateRecords(): List<ImportedEntryRecord>

    override suspend fun getAllEntriesByDate(): List<ImportedEntry> =
        getAllEntriesByDateRecords().map { it.toDomain() }

    @Query("DELETE FROM imported_entries")
    override suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM imported_entries")
    override suspend fun count(): Int

    @Query("""
        SELECT category, SUM(amount) AS total FROM imported_entries
        WHERE is_expense = :isExpense
        AND (:category IS NULL OR category = :category)
        AND date_millis BETWEEN :startMillis AND :endMillis
        GROUP BY category ORDER BY total DESC
    """)
    override suspend fun getCategoryTotals(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<CategoryTotal>

    @Query("""
        SELECT * FROM imported_entries
        WHERE is_expense = :isExpense
        AND (:category IS NULL OR category = :category)
        AND date_millis BETWEEN :startMillis AND :endMillis
        ORDER BY date_millis ASC
    """)
    suspend fun getEntriesByDateRangeRecords(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<ImportedEntryRecord>

    override suspend fun getEntriesByDateRange(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
    ): List<ImportedEntry> =
        getEntriesByDateRangeRecords(isExpense, category, startMillis, endMillis).map { it.toDomain() }

    @Query("SELECT MIN(date_millis) FROM imported_entries")
    override suspend fun getMinDate(): Long?

    @Query("SELECT MAX(date_millis) FROM imported_entries")
    override suspend fun getMaxDate(): Long?

    @Query("SELECT DISTINCT category FROM imported_entries WHERE category != ''")
    override suspend fun getDistinctCategories(): List<String>

    @Query("""
        SELECT * FROM imported_entries
        WHERE (:category IS NULL OR category = :category)
        AND date_millis BETWEEN :startMillis AND :endMillis
        ORDER BY date_millis DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getEntriesPageRecords(
        category: String?,
        startMillis: Long,
        endMillis: Long,
        limit: Int,
        offset: Int,
    ): List<ImportedEntryRecord>

    override suspend fun getEntriesPage(
        category: String?,
        startMillis: Long,
        endMillis: Long,
        limit: Int,
        offset: Int,
    ): List<ImportedEntry> =
        getEntriesPageRecords(category, startMillis, endMillis, limit, offset).map { it.toDomain() }
}
