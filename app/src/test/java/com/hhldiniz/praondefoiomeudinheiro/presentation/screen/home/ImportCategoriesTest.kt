package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportCategoriesTest {

    private fun entry(category: String) = ImportedEntry(
        dateMillis = 0L,
        amount = 0.0,
        description = "",
        category = category,
        isExpense = true,
        fileName = "",
    )

    @Test
    fun derivesNewCategoriesIgnoringBlanksAndDuplicates() {
        val entries = listOf(
            entry("Alimentacao"),
            entry("Transporte"),
            entry("Alimentacao"),
            entry(""),
            entry("   "),
        )

        val result = deriveCategoriesToInsert(entries, existing = emptySet())

        assertEquals(listOf("Alimentacao", "Transporte"), result)
    }

    @Test
    fun doesNotIncludeCategoriesAlreadyInTable() {
        val entries = listOf(
            entry("Alimentacao"),
            entry("Lazer"),
        )

        val result = deriveCategoriesToInsert(entries, existing = setOf("Alimentacao"))

        assertEquals(listOf("Lazer"), result)
    }

    @Test
    fun derivesCategoriesFromFullFileEvenWhenAllEntriesAreDuplicates() {
        val entries = listOf(
            entry("Alimentacao"),
            entry("Transporte"),
        )

        val result = deriveCategoriesToInsert(entries, existing = setOf("Alimentacao"))

        assertEquals(listOf("Transporte"), result)
    }

    @Test
    fun returnsEmptyWhenNoNewCategories() {
        val entries = listOf(entry("Alimentacao"), entry(""))

        val result = deriveCategoriesToInsert(entries, existing = setOf("Alimentacao"))

        assertEquals(emptyList<String>(), result)
    }
}

private class FakeCategoryDao(initial: List<String> = emptyList()) : CategoryDao {
    private val stored = initial.toMutableList()
    val inserted: List<String> get() = stored.toList()
    override fun getAll(): Flow<List<Category>> =
        flowOf(stored.map { Category(name = it) })
    override suspend fun getAllSync(): List<Category> =
        stored.map { Category(name = it) }
    override suspend fun insert(category: Category): Long {
        stored.add(category.name)
        return stored.size.toLong()
    }
    override suspend fun insertAll(categories: List<Category>) {
        stored.addAll(categories.map { it.name })
    }
    override suspend fun deleteAll() = stored.clear()
}

class SaveCategoriesTest {

    private fun entry(category: String) = ImportedEntry(
        dateMillis = 0L,
        amount = 0.0,
        description = "",
        category = category,
        isExpense = true,
        fileName = "",
    )

    @Test
    fun savesOnlyMissingCategoriesFromImportedEntries() = runTest {
        val dao = FakeCategoryDao(initial = listOf("Alimentacao"))
        val entries = listOf(
            entry("Alimentacao"),
            entry("Transporte"),
            entry("Lazer"),
            entry("Transporte"),
            entry(""),
        )

        val existing = dao.getAllSync().map { it.name }.toSet()
        deriveCategoriesToInsert(entries, existing).forEach { dao.insert(Category(name = it)) }

        assertEquals(
            listOf("Alimentacao", "Transporte", "Lazer"),
            dao.inserted,
        )
    }

    @Test
    fun savesNoCategoriesWhenAllAlreadyRegistered() = runTest {
        val dao = FakeCategoryDao(initial = listOf("Alimentacao", "Transporte"))
        val entries = listOf(entry("Alimentacao"), entry("Transporte"))

        val existing = dao.getAllSync().map { it.name }.toSet()
        deriveCategoriesToInsert(entries, existing).forEach { dao.insert(Category(name = it)) }

        assertEquals(listOf("Alimentacao", "Transporte"), dao.inserted)
    }

    @Test
    fun savesCategoriesFromFullFileEvenWhenAllEntriesAreDuplicates() = runTest {
        val dao = FakeCategoryDao(initial = listOf("Alimentacao"))
        val entries = listOf(entry("Alimentacao"), entry("Transporte"))

        val existing = dao.getAllSync().map { it.name }.toSet()
        deriveCategoriesToInsert(entries, existing).forEach { dao.insert(Category(name = it)) }

        assertEquals(listOf("Alimentacao", "Transporte"), dao.inserted)
    }
}
