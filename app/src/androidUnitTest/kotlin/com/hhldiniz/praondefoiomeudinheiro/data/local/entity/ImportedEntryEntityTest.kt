package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /**
     * `importedAt` defaults to "now", and Json omits a property whose value
     * equals its default — a default that is re-evaluated at encode time, so
     * without `@EncodeDefault` an entry serialized in the same millisecond it
     * was built went to `localStorage` without its timestamp and came back
     * carrying the load time instead (see the identical case pinned for
     * `Investment.updatedAt`).
     */
    @Test
    fun json_writesImportedAtEvenWhenItHoldsItsDefault() {
        val encoded = Json.encodeToString(
            ImportedEntry(
                dateMillis = 1L,
                amount = 10.0,
                description = "d",
                category = "c",
                isExpense = false,
            )
        )

        assertTrue(encoded, encoded.contains("\"importedAt\""))
    }

    @Test
    fun json_roundTripsExplicitImportedAt() {
        val original = ImportedEntry(
            id = 5,
            dateMillis = 1_700_000_000_000L,
            amount = 42.5,
            description = "Mercado",
            category = "Alimentacao",
            isExpense = true,
            fileName = "extrato.csv",
            importedAt = 1_700_000_900_000L,
        )

        val decoded = Json.decodeFromString<ImportedEntry>(Json.encodeToString(original))

        assertEquals(original, decoded)
    }
}
