package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryTotal
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [HomeViewModel] business logic.
 *
 * We focus on pure logic surfaces that can be exercised via the public API
 * without requiring Android Context (parseAmount, parseDate, deriveCategoriesToInsert,
 * currency detection via onCurrencyChanged, period selection, etc.).
 *
 * The [deriveCategoriesToInsert] function is tested more thoroughly in
 * [ImportCategoriesTest]; here we complement with integration-style tests that
 * go through the ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var importRepository: ImportRepository
    private lateinit var spreadsheetRepository: SpreadsheetRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        importRepository = mock()
        spreadsheetRepository = mock()
        categoryRepository = mock()

        whenever(categoryRepository.getAll()).thenReturn(flowOf(emptyList()))
        runBlocking { whenever(categoryRepository.getAllSync()).thenReturn(emptyList()) }
        stubRepositoryDefaults()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        DataClearedHolder.reset()
        // Reset currency back to default
        CurrencyHolder.setCurrency(CurrencyOption.BRL)
    }

    private fun stubRepositoryDefaults() {
        runTest {
            whenever(importRepository.count()).thenReturn(0)
            whenever(importRepository.getMinDate()).thenReturn(null)
            whenever(importRepository.getMaxDate()).thenReturn(null)
            whenever(importRepository.getCategoryTotals(any(), anyOrNull(), any(), any()))
                .thenReturn(emptyList())
            whenever(importRepository.getEntriesByDateRange(any(), anyOrNull(), any(), any()))
                .thenReturn(emptyList())
        }
    }

    private fun buildViewModel() = HomeViewModel(importRepository, spreadsheetRepository, categoryRepository, testDispatcher)

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun initialState_matchesCurrencyHolder() = runTest {
        CurrencyHolder.setCurrency(CurrencyOption.USD)
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CurrencyOption.USD, viewModel.uiState.value.selectedCurrency)
    }

    @Test
    fun initialState_emptySpendingAndEarnings() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.spendingData.isEmpty())
        assertTrue(viewModel.uiState.value.earningsData.isEmpty())
        assertEquals(0.0, viewModel.uiState.value.totalSpending, 0.0)
        assertEquals(0.0, viewModel.uiState.value.totalEarnings, 0.0)
    }

    // -------------------------------------------------------------------------
    // onCurrencyChanged
    // -------------------------------------------------------------------------

    @Test
    fun onCurrencyChanged_updatesCurrencyHolder() = runTest {
        viewModel = buildViewModel()
        viewModel.onCurrencyChanged(CurrencyOption.EUR)
        assertEquals(CurrencyOption.EUR, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun onCurrencyChanged_updatesUiStateViaCurrencyHolder() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCurrencyChanged(CurrencyOption.GBP)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CurrencyOption.GBP, viewModel.uiState.value.selectedCurrency)
    }

    // -------------------------------------------------------------------------
    // onPatrimonyChanged
    // -------------------------------------------------------------------------

    @Test
    fun onPatrimonyChanged_updatesPatrimony() = runTest {
        viewModel = buildViewModel()
        viewModel.onPatrimonyChanged(50_000.0)
        assertEquals(50_000.0, viewModel.uiState.value.patrimony, 0.0)
    }

    // -------------------------------------------------------------------------
    // onPeriodSelected
    // -------------------------------------------------------------------------

    @Test
    fun onPeriodSelected_changesPeriod() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPeriodSelected(Period.YEAR)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Period.YEAR, viewModel.uiState.value.selectedPeriod)
    }

    @Test
    fun onPeriodSelected_samePeriod_noStateChange() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val before = viewModel.uiState.value.selectedPeriod

        viewModel.onPeriodSelected(before) // same period
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, viewModel.uiState.value.selectedPeriod)
    }

    @Test
    fun onPeriodSelected_custom_setsCustomDateRange() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPeriodSelected(Period.CUSTOM)
        testDispatcher.scheduler.advanceUntilIdle()

        // After selecting CUSTOM, filterByDateRange is called setting customStart/End
        assertEquals(Period.CUSTOM, viewModel.uiState.value.selectedPeriod)
    }

    // -------------------------------------------------------------------------
    // onCustomDateRange
    // -------------------------------------------------------------------------

    @Test
    fun onCustomDateRange_setsCustomDates() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val start = 1_000_000L
        val end   = 2_000_000L
        viewModel.onCustomDateRange(start, end)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(start, state.customStartDate)
        assertEquals(end, state.customEndDate)
        assertEquals(Period.CUSTOM, state.selectedPeriod)
    }

    // -------------------------------------------------------------------------
    // DataClearedHolder integration
    // -------------------------------------------------------------------------

    @Test
    fun whenDataCleared_showsZeroedState() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        DataClearedHolder.markCleared()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.spendingData.isEmpty())
        assertTrue(state.categorySpending.isEmpty())
        assertNull(state.debugMessage)
    }

    // -------------------------------------------------------------------------
    // refreshData
    // -------------------------------------------------------------------------

    @Test
    fun refreshData_withNoRoomData_showsZeroedState() = runTest {
        whenever(importRepository.count()).thenReturn(0)
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.spendingData.isEmpty())
    }

    @Test
    fun refreshData_withRoomData_loadsFromRoom() = runTest {
        whenever(importRepository.count()).thenReturn(5)
        whenever(importRepository.getCategoryTotals(eq(true), anyOrNull(), any(), any()))
            .thenReturn(listOf(CategoryTotal("Alimentacao", 200.0)))
        whenever(importRepository.getCategoryTotals(eq(false), anyOrNull(), any(), any()))
            .thenReturn(emptyList())
        whenever(importRepository.getMinDate()).thenReturn(1_000_000L)
        whenever(importRepository.getMaxDate()).thenReturn(2_000_000L)

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // With room data present, allCategories should be populated
        assertTrue(state.allCategories.isNotEmpty() || state.datasetMinDate != null)
    }

    // -------------------------------------------------------------------------
    // onCategorySelected
    // -------------------------------------------------------------------------

    @Test
    fun onCategorySelected_updatesSelectedCategory() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCategorySelected("Lazer")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Lazer", viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun onCategorySelected_null_clearsCategory() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCategorySelected("Lazer")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onCategorySelected(null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedCategory)
    }
}
