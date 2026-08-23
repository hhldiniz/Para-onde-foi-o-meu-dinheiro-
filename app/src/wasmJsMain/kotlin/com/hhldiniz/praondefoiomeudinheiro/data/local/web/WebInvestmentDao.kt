package com.hhldiniz.praondefoiomeudinheiro.data.local.web

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.InvestmentDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val STORAGE_KEY = "praondefoiomeudinheiro.investments"

/**
 * [InvestmentDao] backed by `localStorage`; see [WebImportedEntryDao] for the
 * persistence approach, including why read/write failures are caught and
 * logged instead of thrown.
 *
 * The ordering matches `RoomInvestmentDao`'s
 * `ORDER BY date_millis DESC, id DESC`.
 */
class WebInvestmentDao : InvestmentDao {

    private val state = MutableStateFlow(load())

    private fun load(): List<Investment> {
        val raw = runCatching { localStorage.getItem(STORAGE_KEY) }
            .onFailure { logStorageError("load investments", it) }
            .getOrNull() ?: return emptyList()
        return runCatching { Json.decodeFromString<List<Investment>>(raw) }
            .onFailure { logStorageError("decode investments", it) }
            .getOrDefault(emptyList())
    }

    private fun persist(investments: List<Investment>) {
        runCatching { localStorage.setItem(STORAGE_KEY, Json.encodeToString(investments)) }
            .onFailure { logStorageError("persist investments", it) }
    }

    private fun List<Investment>.sorted(): List<Investment> =
        sortedWith(compareByDescending<Investment> { it.dateMillis }.thenByDescending { it.id })

    override fun getAll(): Flow<List<Investment>> = state.map { it.sorted() }

    override suspend fun getAllSync(): List<Investment> = state.value.sorted()

    override suspend fun insert(investment: Investment): Long {
        val current = state.value
        val id = (current.maxOfOrNull { it.id } ?: 0L) + 1
        val updated = current + investment.copy(id = id)
        state.value = updated
        persist(updated)
        return id
    }

    override suspend fun update(investment: Investment) {
        val current = state.value
        if (current.none { it.id == investment.id }) return
        val updated = current.map { if (it.id == investment.id) investment else it }
        state.value = updated
        persist(updated)
    }

    override suspend fun deleteById(id: Long) {
        val updated = state.value.filterNot { it.id == id }
        state.value = updated
        persist(updated)
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
        persist(emptyList())
    }

    override suspend fun count(): Int = state.value.size
}
