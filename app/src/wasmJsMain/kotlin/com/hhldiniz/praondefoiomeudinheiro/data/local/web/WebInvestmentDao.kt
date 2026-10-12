package com.hhldiniz.praondefoiomeudinheiro.data.local.web

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.InvestmentDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [InvestmentDao] backed by `localStorage`; see [WebImportedEntryDao] for the
 * persistence approach, including why read/write failures are caught and
 * logged instead of thrown.
 *
 * The ordering matches `RoomInvestmentDao`'s
 * `ORDER BY date_millis DESC, id DESC`.
 */
class WebInvestmentDao : InvestmentDao {

    private val table = localStorageTable<Investment>("praondefoiomeudinheiro.investments", "investments")

    private fun List<Investment>.sorted(): List<Investment> =
        sortedWith(compareByDescending<Investment> { it.dateMillis }.thenByDescending { it.id })

    override fun getAll(): Flow<List<Investment>> = table.flow.map { it.sorted() }

    override suspend fun getAllSync(): List<Investment> = table.rows.sorted()

    override suspend fun insert(investment: Investment): Long {
        val current = table.rows
        val id = (current.maxOfOrNull { it.id } ?: 0L) + 1
        table.write(current + investment.copy(id = id))
        return id
    }

    override suspend fun update(investment: Investment) {
        val current = table.rows
        if (current.none { it.id == investment.id }) return
        table.write(current.map { if (it.id == investment.id) investment else it })
    }

    override suspend fun deleteById(id: Long) {
        table.write(table.rows.filterNot { it.id == id })
    }

    override suspend fun deleteAll() {
        table.write(emptyList())
    }

    override suspend fun count(): Int = table.rows.size
}
