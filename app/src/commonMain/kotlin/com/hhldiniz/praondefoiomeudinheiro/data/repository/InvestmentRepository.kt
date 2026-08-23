package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.InvestmentDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import kotlinx.coroutines.flow.Flow

/**
 * Repository that wraps [InvestmentDao]. Positions are entered by hand, so
 * there is no import/dedupe step here — unlike [ImportRepository] this is a
 * plain CRUD surface, and the aggregation the investments tab shows (totals,
 * allocation per type) is derived in the ViewModel from the same list the UI
 * already holds rather than by a second round trip to storage.
 */
class InvestmentRepository(private val dao: InvestmentDao) {

    /** Every position, newest first, as a reactive Flow. */
    fun getAll(): Flow<List<Investment>> = dao.getAll()

    suspend fun getAllSync(): List<Investment> = dao.getAllSync()

    suspend fun insert(investment: Investment): Long = dao.insert(investment)

    suspend fun update(investment: Investment) = dao.update(investment)

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun count(): Int = dao.count()
}
