package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionColumnMapperTest {

    @Test
    fun findColumnGroups_canonicalPortugueseHeader_mapsAllFourColumns() {
        val groups = TransactionColumnMapper.findColumnGroups(listOf("Data", "Valor", "Descrição", "Categoria"))

        assertEquals(1, groups.size)
        val group = groups.single()
        assertEquals(0, group.dateCol)
        assertEquals(1, group.amountCol)
        assertEquals(2, group.descriptionCol)
        assertEquals(3, group.categoryCol)
        assertTrue(group.isExpense)
    }

    @Test
    fun findColumnGroups_englishHeader_isRecognized() {
        val group = TransactionColumnMapper.findColumnGroups(listOf("Date", "Amount", "Description", "Category")).single()

        assertEquals(0, group.dateCol)
        assertEquals(1, group.amountCol)
        assertEquals(2, group.descriptionCol)
        assertEquals(3, group.categoryCol)
    }

    @Test
    fun findColumnGroups_spanishHeaderOutOfOrder_findsEachColumnByName() {
        // Category before date, amount last — an order the old fixed-index
        // logic could never have handled.
        val groups = TransactionColumnMapper.findColumnGroups(listOf("Categoría", "Fecha", "Descripción", "Valor"))

        assertEquals(1, groups.size)
        val group = groups.single()
        assertEquals(1, group.dateCol)
        assertEquals(3, group.amountCol)
        assertEquals(2, group.descriptionCol)
        assertEquals(0, group.categoryCol)
    }

    @Test
    fun findColumnGroups_missingCategory_leavesCategoryColNull() {
        val group = TransactionColumnMapper.findColumnGroups(listOf("Fecha", "Valor", "Descripción")).single()

        assertEquals(2, group.descriptionCol)
        assertNull(group.categoryCol)
    }

    @Test
    fun findColumnGroups_noDateColumn_returnsNoGroups() {
        val groups = TransactionColumnMapper.findColumnGroups(listOf("Foo", "Bar", "Baz"))

        assertTrue(groups.isEmpty())
    }

    @Test
    fun findColumnGroups_dateWithoutAmount_isNotATable() {
        val groups = TransactionColumnMapper.findColumnGroups(listOf("Data", "Descrição", "Categoria"))

        assertTrue(groups.isEmpty())
    }

    @Test
    fun findColumnGroups_twoTablesSideBySide_alternatesExpenseAndIncome() {
        val groups = TransactionColumnMapper.findColumnGroups(
            listOf("Despesas", "Data", "Valor", "Descrição", "Categoria", "Renda", "Data", "Valor", "Descrição", "Categoria")
        )

        assertEquals(2, groups.size)
        assertTrue(groups[0].isExpense)
        assertEquals(1, groups[0].dateCol)
        assertEquals(2, groups[0].amountCol)
        assertTrue(!groups[1].isExpense)
        assertEquals(6, groups[1].dateCol)
        assertEquals(7, groups[1].amountCol)
    }

    @Test
    fun findHeaderRowIndex_skipsRowsWithoutRecognizableColumns() {
        val rows = listOf(
            listOf("Budget", "", ""),
            listOf("Data", "Valor", "Categoria"),
            listOf("01/01/2026", "10", "Food"),
        )

        assertEquals(1, TransactionColumnMapper.findHeaderRowIndex(rows))
    }

    @Test
    fun findHeaderRowIndex_noRecognizableRow_returnsMinusOne() {
        val rows = listOf(listOf("Foo", "Bar"), listOf("1", "2"))

        assertEquals(-1, TransactionColumnMapper.findHeaderRowIndex(rows))
    }

    @Test
    fun extractEntry_rowShorterThanAmountColumn_returnsNull() {
        val group = TransactionColumnMapper.findColumnGroups(listOf("Data", "Valor", "Descrição", "Categoria")).single()

        val entry = TransactionColumnMapper.extractEntry(listOf("01/01/2026"), group)

        assertNull(entry)
    }

    @Test
    fun extractEntry_blankAmount_returnsNull() {
        val group = TransactionColumnMapper.findColumnGroups(listOf("Data", "Valor", "Descrição", "Categoria")).single()

        val entry = TransactionColumnMapper.extractEntry(listOf("01/01/2026", "  ", "Café", "Alimentação"), group)

        assertNull(entry)
    }

    @Test
    fun extractEntry_missingOptionalColumns_defaultToBlank() {
        val group = TransactionColumnMapper.findColumnGroups(listOf("Fecha", "Valor")).single()

        val entry = TransactionColumnMapper.extractEntry(listOf("01/01/2026", "10"), group)

        assertEquals(CsvEntry("01/01/2026", "10", "", ""), entry)
    }
}
