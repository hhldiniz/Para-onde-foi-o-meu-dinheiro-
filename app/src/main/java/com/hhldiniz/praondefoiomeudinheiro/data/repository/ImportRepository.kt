package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.coroutines.flow.Flow

class ImportRepository(private val dao: ImportedEntryDao) {

    suspend fun insertEntries(entries: List<ImportedEntry>): List<ImportedEntry> {
        val results = dao.insertAll(entries)
        return entries.filterIndexed { index, _ -> results[index] != -1L }
    }

    fun getAllEntries(): Flow<List<ImportedEntry>> = dao.getAllEntries()

    fun getSpendingEntries(): Flow<List<ImportedEntry>> = dao.getSpendingEntries()

    fun getEarningsEntries(): Flow<List<ImportedEntry>> = dao.getEarningsEntries()

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun count(): Int = dao.count()
}
