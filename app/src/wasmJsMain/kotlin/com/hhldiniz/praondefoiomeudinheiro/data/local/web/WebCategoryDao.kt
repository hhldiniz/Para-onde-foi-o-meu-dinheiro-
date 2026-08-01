package com.hhldiniz.praondefoiomeudinheiro.data.local.web

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val STORAGE_KEY = "praondefoiomeudinheiro.categories"

/**
 * [CategoryDao] backed by `localStorage`; see [WebImportedEntryDao] for the
 * persistence approach, including why read/write failures are caught and
 * logged instead of thrown.
 */
class WebCategoryDao : CategoryDao {

    private val state = MutableStateFlow(load())

    private fun load(): List<Category> {
        val raw = runCatching { localStorage.getItem(STORAGE_KEY) }
            .onFailure { logStorageError("load categories", it) }
            .getOrNull() ?: return emptyList()
        return runCatching { Json.decodeFromString<List<Category>>(raw) }
            .onFailure { logStorageError("decode categories", it) }
            .getOrDefault(emptyList())
    }

    private fun persist(categories: List<Category>) {
        runCatching { localStorage.setItem(STORAGE_KEY, Json.encodeToString(categories)) }
            .onFailure { logStorageError("persist categories", it) }
    }

    override fun getAll(): Flow<List<Category>> = state.map { it.sortedBy(Category::name) }

    override suspend fun getAllSync(): List<Category> = state.value.sortedBy(Category::name)

    override suspend fun insert(category: Category): Long {
        val current = state.value
        if (current.any { it.name == category.name }) return -1L
        val id = (current.maxOfOrNull { it.id } ?: 0L) + 1
        val updated = current + category.copy(id = id)
        state.value = updated
        persist(updated)
        return id
    }

    override suspend fun insertAll(categories: List<Category>) {
        val current = state.value
        var nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1
        val existingNames = current.mapTo(mutableSetOf()) { it.name }
        val toAdd = mutableListOf<Category>()
        for (category in categories) {
            if (existingNames.add(category.name)) {
                toAdd += category.copy(id = nextId++)
            }
        }
        if (toAdd.isNotEmpty()) {
            val updated = current + toAdd
            state.value = updated
            persist(updated)
        }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
        persist(emptyList())
    }

    override suspend fun deleteByName(name: String) {
        val updated = state.value.filterNot { it.name == name }
        state.value = updated
        persist(updated)
    }
}
