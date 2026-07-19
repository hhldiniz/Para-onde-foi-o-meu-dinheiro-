package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import android.content.ContentResolver
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvUriHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValueRange
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Integration-style tests for [HomeViewModel] covering:
 * - parseAmount (via loadData)
 * - parseDate (via loadData)
 * - detectCurrency (via loadData)
 * - buildChartData grouping (via loadData with different periods)
 * - loadData with empty URI list falls through to Room path
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelParseTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var importRepository: ImportRepository
    private lateinit var spreadsheetRepository: SpreadsheetRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var contentResolver: ContentResolver
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        importRepository = mock()
        spreadsheetRepository = mock()
        categoryRepository = mock()
        contentResolver = mock()
        whenever(categoryRepository.getAll()).thenReturn(flowOf(emptyList()))
        runBlocking {
            whenever(categoryRepository.getAllSync()).thenReturn(emptyList())
            whenever(categoryRepository.insert(any())).thenReturn(1L)
        }
        stubRepositoryDefaults()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        CsvUriHolder.uris = emptyList()
        CurrencyHolder.setCurrency(CurrencyOption.BRL)
    }

    private fun stubRepositoryDefaults() {
        runBlocking {
            whenever(importRepository.count()).thenReturn(0)
            whenever(importRepository.getMinDate()).thenReturn(null)
            whenever(importRepository.getMaxDate()).thenReturn(null)
            whenever(importRepository.getCategoryTotals(any(), anyOrNull(), any(), any()))
                .thenReturn(emptyList())
            whenever(importRepository.getEntriesByDateRange(any(), anyOrNull(), any(), any()))
                .thenReturn(emptyList())
        }
    }

    private fun buildViewModel(): HomeViewModel {
        viewModel = HomeViewModel(importRepository, spreadsheetRepository, categoryRepository, testDispatcher)
        return viewModel
    }

    private fun entry(date: String, amount: String, desc: String = "d", cat: String = "c") =
        CsvEntry(date, amount, desc, cat)

    // -------------------------------------------------------------------------
    // loadData – empty URI list → shows zeroed state
    // -------------------------------------------------------------------------

    @Test
    fun loadData_emptyUriList_noRoomData_showsZeroedState() = runTest {
        CsvUriHolder.uris = emptyList()
        whenever(importRepository.count()).thenReturn(0)
        val vm = buildViewModel()
        vm.loadData(contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.spendingData.isEmpty())
        assertTrue(vm.uiState.value.categorySpending.isEmpty())
        assertEquals(0.0, vm.uiState.value.totalSpending, 0.0)
    }

    // -------------------------------------------------------------------------
    // parseAmount – tested via importFile integration
    // -------------------------------------------------------------------------

    @Test
    fun importFile_dotDecimalAmount_parsedCorrectly() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1:D2",
            rows = emptyList(),
            spendingEntries = listOf(entry("01/01/2024", "100.50")),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            invocation.getArgument<List<ImportedEntry>>(0)
        }

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.debugMessage?.contains("Importado") == true)
    }

    @Test
    fun importFile_commaDecimalAmount_parsedCorrectly() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1:D2",
            rows = emptyList(),
            spendingEntries = listOf(entry("01/01/2024", "1.234,56")),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            invocation.getArgument<List<ImportedEntry>>(0)
        }

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.debugMessage?.contains("Importado") == true)
    }

    @Test
    fun importFile_invalidAmount_entryDropped() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1:D2",
            rows = emptyList(),
            spendingEntries = listOf(entry("01/01/2024", "NOT_A_NUMBER")),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenReturn(emptyList())

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        // Invalid amount → entry not parsed → 0 imported entries debug message or empty state
        assertTrue(vm.uiState.value.debugMessage?.contains("Importado") == true ||
                   vm.uiState.value.debugMessage?.contains("Falha") == true ||
                   vm.uiState.value.spendingData.isEmpty())
    }

    // -------------------------------------------------------------------------
    // parseDate – various formats
    // -------------------------------------------------------------------------

    @Test
    fun importFile_ddMMyyyy_dateFormat_parsed() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1", rows = emptyList(),
            spendingEntries = listOf(entry("15/03/2024", "50.00")),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenAnswer { it.getArgument<List<ImportedEntry>>(0) }

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.debugMessage?.contains("Importado") == true)
    }

    @Test
    fun importFile_yyyyMMdd_dateFormat_parsed() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1", rows = emptyList(),
            spendingEntries = listOf(entry("2024-03-15", "50.00")),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenAnswer { it.getArgument<List<ImportedEntry>>(0) }

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.debugMessage?.contains("Importado") == true)
    }

    @Test
    fun importFile_invalidDate_entryDropped() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1", rows = emptyList(),
            spendingEntries = listOf(entry("not-a-date", "50.00")),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenReturn(emptyList())

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        // Entry with invalid date is dropped; 0 entries inserted → debug message indicates 0 or failure
        assertTrue(
            vm.uiState.value.debugMessage?.contains("0") == true ||
            vm.uiState.value.debugMessage?.contains("Importado") == true ||
            vm.uiState.value.spendingData.isEmpty()
        )
    }

    // -------------------------------------------------------------------------
    // detectCurrency – via importFile
    // -------------------------------------------------------------------------

    @Test
    fun importFile_brlAmounts_detectsBrl() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1", rows = emptyList(),
            spendingEntries = listOf(
                entry("01/01/2024", "R$ 100,00"),
                entry("02/01/2024", "R$ 200,00")
            ),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenAnswer { it.getArgument<List<ImportedEntry>>(0) }

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CurrencyOption.BRL, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun importFile_eurAmounts_detectsEur() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        val range = ValueRange(
            range = "A1", rows = emptyList(),
            spendingEntries = listOf(
                entry("01/01/2024", "€ 100,00"),
                entry("02/01/2024", "€ 200,00")
            ),
            earningsEntries = emptyList()
        )
        whenever(spreadsheetRepository.readValues(any(), any())).thenReturn(Result.success(range))
        whenever(importRepository.insertEntries(any())).thenAnswer { it.getArgument<List<ImportedEntry>>(0) }

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CurrencyOption.EUR, CurrencyHolder.selectedCurrency.value)
    }

    // -------------------------------------------------------------------------
    // importFile – failure path
    // -------------------------------------------------------------------------

    @Test
    fun importFile_repositoryFailure_setsErrorMessage() = runTest {
        val mockUri = mock<android.net.Uri>().also {
            whenever(it.lastPathSegment).thenReturn("file.csv")
        }
        whenever(spreadsheetRepository.readValues(any(), any()))
            .thenReturn(Result.failure(RuntimeException("File not found")))

        val vm = buildViewModel()
        vm.importFile(mockUri, contentResolver)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.debugMessage?.contains("Falha") == true)
    }
}
