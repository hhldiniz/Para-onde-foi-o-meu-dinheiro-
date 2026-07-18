package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportedEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<ImportedEntry>): List<Long>

    @Query("SELECT * FROM imported_entries ORDER BY date_millis DESC")
    fun getAllEntries(): Flow<List<ImportedEntry>>

    @Query("SELECT * FROM imported_entries WHERE is_expense = 1 ORDER BY date_millis DESC")
    fun getSpendingEntries(): Flow<List<ImportedEntry>>

    @Query("SELECT * FROM imported_entries WHERE is_expense = 0 ORDER BY date_millis DESC")
    fun getEarningsEntries(): Flow<List<ImportedEntry>>

    @Query("DELETE FROM imported_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM imported_entries")
    suspend fun count(): Int
}
