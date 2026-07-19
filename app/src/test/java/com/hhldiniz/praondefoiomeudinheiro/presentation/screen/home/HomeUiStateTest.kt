package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HomeUiState], [SpendingDataPoint], [CategorySpending],
 * [Period], and [EntryDisplay] data models.
 */
class HomeUiStateTest {

    // -------------------------------------------------------------------------
    // HomeUiState
    // -------------------------------------------------------------------------

    @Test
    fun homeUiState_defaultValues() {
        val state = HomeUiState()
        assertTrue(state.spendingData.isEmpty())
        assertTrue(state.categorySpending.isEmpty())
        assertTrue(state.earningsData.isEmpty())
        assertTrue(state.categoryEarnings.isEmpty())
        assertEquals(Period.MONTH, state.selectedPeriod)
        assertEquals(0.0, state.totalSpending, 0.0)
        assertEquals(0.0, state.totalEarnings, 0.0)
        assertNull(state.customStartDate)
        assertNull(state.customEndDate)
        assertEquals(100_000.0, state.patrimony, 0.0)
        assertEquals(CurrencyOption.BRL, state.selectedCurrency)
        assertNull(state.debugMessage)
        assertTrue(state.allCategories.isEmpty())
        assertNull(state.selectedCategory)
        assertNull(state.datasetMinDate)
        assertNull(state.datasetMaxDate)
    }

    @Test
    fun homeUiState_copyPreservesChangedFields() {
        val base = HomeUiState()
        val updated = base.copy(
            totalSpending = 500.0,
            totalEarnings = 1000.0,
            selectedPeriod = Period.YEAR,
            selectedCurrency = CurrencyOption.USD,
        )
        assertEquals(500.0, updated.totalSpending, 0.0)
        assertEquals(1000.0, updated.totalEarnings, 0.0)
        assertEquals(Period.YEAR, updated.selectedPeriod)
        assertEquals(CurrencyOption.USD, updated.selectedCurrency)
        // unchanged fields
        assertEquals(100_000.0, updated.patrimony, 0.0)
    }

    @Test
    fun homeUiState_withSpendingAndEarnings() {
        val state = HomeUiState(
            spendingData = listOf(SpendingDataPoint("Jan", 200.0)),
            earningsData = listOf(SpendingDataPoint("Jan", 400.0)),
            totalSpending = 200.0,
            totalEarnings = 400.0,
        )
        assertEquals(1, state.spendingData.size)
        assertEquals(1, state.earningsData.size)
        assertEquals(200.0, state.totalSpending, 0.0)
        assertEquals(400.0, state.totalEarnings, 0.0)
    }

    // -------------------------------------------------------------------------
    // SpendingDataPoint
    // -------------------------------------------------------------------------

    @Test
    fun spendingDataPoint_holdsLabelAndValue() {
        val dp = SpendingDataPoint("Mar", 1234.56)
        assertEquals("Mar", dp.label)
        assertEquals(1234.56, dp.value, 0.0001)
    }

    @Test
    fun spendingDataPoint_equalityByFields() {
        val a = SpendingDataPoint("Jan", 100.0)
        val b = SpendingDataPoint("Jan", 100.0)
        assertEquals(a, b)
    }

    @Test
    fun spendingDataPoint_withZeroValue() {
        val dp = SpendingDataPoint("", 0.0)
        assertEquals(0.0, dp.value, 0.0)
    }

    // -------------------------------------------------------------------------
    // CategorySpending
    // -------------------------------------------------------------------------

    @Test
    fun categorySpending_holdsFields() {
        val cs = CategorySpending("Alimentacao", 350.0)
        assertEquals("Alimentacao", cs.category)
        assertEquals(350.0, cs.value, 0.0001)
    }

    @Test
    fun categorySpending_equalityByFields() {
        val a = CategorySpending("Lazer", 50.0)
        val b = CategorySpending("Lazer", 50.0)
        assertEquals(a, b)
    }

    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------

    @Test
    fun period_enumValuesExist() {
        val values = Period.entries
        assertTrue(values.contains(Period.DAY))
        assertTrue(values.contains(Period.WEEK))
        assertTrue(values.contains(Period.MONTH))
        assertTrue(values.contains(Period.YEAR))
        assertTrue(values.contains(Period.CUSTOM))
    }

    @Test
    fun period_uniqueOrdinals() {
        val ordinals = Period.entries.map { it.ordinal }
        assertEquals(ordinals.toSet().size, ordinals.size)
    }

    @Test
    fun period_eachHasLabelRes() {
        Period.entries.forEach { period ->
            // labelRes is an @StringRes Int; it must be non-zero
            assertFalse("Period $period should have a non-zero labelRes", period.labelRes == 0)
        }
    }

    // -------------------------------------------------------------------------
    // EntryDisplay
    // -------------------------------------------------------------------------

    @Test
    fun entryDisplay_holdsAllFields() {
        val entry = EntryDisplay(
            dateMillis = 1_700_000_000_000L,
            description = "Restaurante",
            category = "Alimentacao",
            amount = 45.90,
            isExpense = true,
        )
        assertEquals(1_700_000_000_000L, entry.dateMillis)
        assertEquals("Restaurante", entry.description)
        assertEquals("Alimentacao", entry.category)
        assertEquals(45.90, entry.amount, 0.001)
        assertTrue(entry.isExpense)
    }

    @Test
    fun entryDisplay_earnings_isExpenseFalse() {
        val entry = EntryDisplay(
            dateMillis = 1L,
            description = "Salario",
            category = "Salario",
            amount = 5000.0,
            isExpense = false,
        )
        assertFalse(entry.isExpense)
    }

    @Test
    fun entryDisplay_equality() {
        val a = EntryDisplay(1L, "d", "c", 10.0, true)
        val b = EntryDisplay(1L, "d", "c", 10.0, true)
        assertEquals(a, b)
    }
}
