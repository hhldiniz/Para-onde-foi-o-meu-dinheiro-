package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
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
