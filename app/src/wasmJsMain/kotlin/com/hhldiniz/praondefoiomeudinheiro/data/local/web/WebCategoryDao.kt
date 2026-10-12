package com.hhldiniz.praondefoiomeudinheiro.data.local.web

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [CategoryDao] backed by `localStorage`; see [WebImportedEntryDao] for the
 * persistence approach, including why read/write failures are caught and
 * logged instead of thrown.
 */
class WebCategoryDao : CategoryDao {

    private val table = localStorageTable<Category>("praondefoiomeudinheiro.categories", "categories")

    override fun getAll(): Flow<List<Category>> = table.flow.map { it.sortedBy(Category::name) }

    override suspend fun getAllSync(): List<Category> = table.rows.sortedBy(Category::name)

    override suspend fun insert(category: Category): Long {
        val current = table.rows
        if (current.any { it.name == category.name }) return -1L
        val id = (current.maxOfOrNull { it.id } ?: 0L) + 1
        table.write(current + category.copy(id = id))
        return id
    }

    override suspend fun insertAll(categories: List<Category>) {
        val current = table.rows
        var nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1
        val existingNames = current.mapTo(mutableSetOf()) { it.name }
        val toAdd = mutableListOf<Category>()
        for (category in categories) {
            if (existingNames.add(category.name)) {
                toAdd += category.copy(id = nextId++)
            }
        }
        if (toAdd.isNotEmpty()) table.write(current + toAdd)
    }

    override suspend fun deleteAll() {
        table.write(emptyList())
    }

    override suspend fun deleteByName(name: String) {
        table.write(table.rows.filterNot { it.name == name })
    }
}
