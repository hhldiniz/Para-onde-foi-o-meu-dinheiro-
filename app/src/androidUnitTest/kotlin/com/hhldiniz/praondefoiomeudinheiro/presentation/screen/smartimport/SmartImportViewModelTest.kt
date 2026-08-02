package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.vision.SmartImportAnalyzer
import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.TransactionField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests the review flow of the automatic import. The analyzer is the real one,
 * driven with a CSV (so no platform text recognizer is involved); only the
 * repositories are doubles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SmartImportViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var importRepository: ImportRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: SmartImportViewModel

    private val statementCsv = """
        Data,Descrição,Categoria,Valor
        15/03/2024,Mercado Bom Preço,Alimentacao,120.50
        16/03/2024,Uber para o trabalho,Transporte,32.00
    """.trimIndent().toByteArray()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        importRepository = mock()
        categoryRepository = mock()
        viewModel = SmartImportViewModel(
            importRepository = importRepository,
            categoryRepository = categoryRepository,
            // The recognizer is never reached on the spreadsheet path.
            analyzer = SmartImportAnalyzer { RecognizedDocument(emptyList()) },
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    private fun pickStatement() {
        viewModel.onFilePicked(InMemoryPlatformFile("extrato.csv", statementCsv))
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun onFilePicked_movesToReviewWithOneCandidatePerTransaction() = runTest {
        pickStatement()

        val state = viewModel.uiState.value
        assertEquals(SmartImportStage.REVIEW, state.stage)
        assertEquals("extrato.csv", state.fileName)
        assertEquals(2, state.candidates.size)
        assertEquals(2, state.selectedCount)
        assertNotNull(state.mappings.firstOrNull { it.field == TransactionField.DATE })
        assertEquals("Mercado Bom Preço", state.candidates[0].transaction.description)
    }

    @Test
    fun onCandidateToggled_dropsAndRestoresACandidate() = runTest {
        pickStatement()

        viewModel.onCandidateToggled(0)
        assertFalse(viewModel.uiState.value.candidates[0].selected)
        assertEquals(1, viewModel.uiState.value.selectedCount)

        viewModel.onCandidateToggled(0)
        assertTrue(viewModel.uiState.value.candidates[0].selected)
    }

    @Test
    fun onCandidateTypeToggled_flipsExpenseAndIncome() = runTest {
        pickStatement()
        assertTrue(viewModel.uiState.value.candidates[0].isExpense)

        viewModel.onCandidateTypeToggled(0)

        assertFalse(viewModel.uiState.value.candidates[0].isExpense)
        // Only that row changes.
        assertTrue(viewModel.uiState.value.candidates[1].isExpense)
    }

    @Test
    fun confirmImport_writesOnlyTheSelectedCandidates() = runTest {
        whenever(importRepository.insertEntries(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            invocation.getArgument<List<ImportedEntry>>(0)
        }
        whenever(categoryRepository.getAllSync()).thenReturn(emptyList())

        pickStatement()
        viewModel.onCandidateToggled(1)
        viewModel.confirmImport()
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = argumentCaptor<List<ImportedEntry>>()
        verify(importRepository).insertEntries(captor.capture())
        val written = captor.firstValue
        assertEquals(1, written.size)
        assertEquals("Mercado Bom Preço", written[0].description)
        assertEquals(120.50, written[0].amount, 0.001)
        assertEquals("extrato.csv", written[0].fileName)
        assertTrue(written[0].isExpense)

        val state = viewModel.uiState.value
        assertEquals(SmartImportStage.DONE, state.stage)
        assertEquals(1, state.importedCount)
        assertEquals(0, state.duplicateCount)
    }

    @Test
    fun confirmImport_countsDuplicatesTheRepositoryRejected() = runTest {
        whenever(importRepository.insertEntries(any())).thenReturn(emptyList())
        whenever(categoryRepository.getAllSync()).thenReturn(emptyList())

        pickStatement()
        viewModel.confirmImport()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SmartImportStage.DONE, state.stage)
        assertEquals(0, state.importedCount)
        assertEquals(2, state.duplicateCount)
    }

    @Test
    fun onFilePicked_fileWithoutTransactions_reportsAnError() = runTest {
        val shoppingList = "Produto,Quantidade\nArroz,2\nFeijão,1".toByteArray()

        viewModel.onFilePicked(InMemoryPlatformFile("lista.csv", shoppingList))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SmartImportStage.IDLE, state.stage)
        assertNotNull(state.error)
        assertTrue(state.candidates.isEmpty())
    }

    @Test
    fun reset_clearsTheAnalysis() = runTest {
        pickStatement()

        viewModel.reset()

        val state = viewModel.uiState.value
        assertEquals(SmartImportStage.IDLE, state.stage)
        assertTrue(state.candidates.isEmpty())
        assertEquals("", state.fileName)
    }
}
