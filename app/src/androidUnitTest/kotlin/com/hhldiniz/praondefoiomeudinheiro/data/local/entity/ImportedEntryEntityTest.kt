package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportedEntryEntityTest {

    @Test
    fun entryHoldsProvidedFields() {
        val entry = ImportedEntry(
            dateMillis = 123L,
            amount = 99.99,
            description = "Compra",
            category = "Lazer",
            isExpense = true,
            fileName = "planilha.csv"
        )
        assertEquals(123L, entry.dateMillis)
        assertEquals(99.99, entry.amount, 0.0001)
        assertEquals("Compra", entry.description)
        assertEquals("Lazer", entry.category)
        assertEquals(true, entry.isExpense)
        assertEquals("planilha.csv", entry.fileName)
    }

    @Test
    fun entryDefaults_areSensible() {
        val entry = ImportedEntry(
            dateMillis = 1L,
            amount = 10.0,
            description = "d",
            category = "c",
            isExpense = false
        )
        assertEquals("", entry.fileName)
        assertEquals(0, entry.id)
    }

    @Test
    fun entryCanBeExpenseOrEarning() {
        val expense = ImportedEntry(
            dateMillis = 1L,
            amount = 1.0,
            description = "d",
            category = "c",
            isExpense = true
        )
        val earning = ImportedEntry(
            dateMillis = 1L,
            amount = 1.0,
            description = "d",
            category = "c",
            isExpense = false
        )
        assertEquals(true, expense.isExpense)
        assertEquals(false, earning.isExpense)
    }
}
