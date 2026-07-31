package com.hhldiniz.praondefoiomeudinheiro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ValueRangeTest {

    private val sampleEntry = CsvEntry(
        date = "2024-01-01",
        amount = "50,00",
        description = "Test",
        category = "Food"
    )

    @Test
    fun csvEntryHoldsFields() {
        assertEquals("2024-01-01", sampleEntry.date)
        assertEquals("50,00", sampleEntry.amount)
        assertEquals("Test", sampleEntry.description)
        assertEquals("Food", sampleEntry.category)
    }

    @Test
    fun valueRangeHoldsRangeAndRows() {
        val rows = listOf(listOf("a", "b"), listOf("c", "d"))
        val range = ValueRange(
            range = "A1:B2",
            rows = rows,
            spendingEntries = listOf(sampleEntry),
            earningsEntries = emptyList()
        )
        assertEquals("A1:B2", range.range)
        assertEquals(rows, range.rows)
        assertEquals(1, range.spendingEntries.size)
        assertEquals(0, range.earningsEntries.size)
    }

    @Test
    fun valueRangeDefaultsToEmptyEntries() {
        val range = ValueRange(range = "A1", rows = emptyList())
        assertEquals(0, range.spendingEntries.size)
        assertEquals(0, range.earningsEntries.size)
    }
}
