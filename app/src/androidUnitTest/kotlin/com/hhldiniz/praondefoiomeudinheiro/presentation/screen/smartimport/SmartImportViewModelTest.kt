package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.vision.SmartImportAnalyzer
import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord
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

    /** Lays a receipt line out the way a text recognizer would report it. */
    private fun line(top: Float, vararg cells: Pair<Float, String>): List<RecognizedWord> =
        cells.flatMap { (start, text) ->
            var x = start
            text.split(" ").map { token ->
                val width = 0.018f * token.length
                val word = RecognizedWord(token, BoundingBox(x, top, x + width, top + 0.03f), 0.9f)
                x += width + 0.008f
                word
            }
        }

    private val photographedReceipt by lazy {
        RecognizedDocument(
            line(0.05f, 0.20f to "Padaria Pao Quente") +
                line(0.11f, 0.20f to "Emissão: 20/05/2024") +
                line(0.20f, 0.05f to "PAO FRANCES", 0.85f to "9,00") +
                line(0.26f, 0.05f to "LEITE INTEGRAL", 0.85f to "6,50") +
                line(0.34f, 0.05f to "VALOR TOTAL R$", 0.85f to "15,50")
        )
    }

    /** A view model whose recognizer always returns [photographedReceipt]. */
    private fun receiptViewModel() = SmartImportViewModel(
        importRepository = importRepository,
        categoryRepository = categoryRepository,
        analyzer = SmartImportAnalyzer { photographedReceipt },
        ioDispatcher = testDispatcher,
    )

    private fun pickReceipt(model: SmartImportViewModel) {
        model.onReceiptPicked(InMemoryPlatformFile("nota.jpg", byteArrayOf(1)))
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
    fun onReceiptPicked_proposesTheTotalAsASingleExpense() = runTest {
        val model = receiptViewModel()

        pickReceipt(model)

        val state = model.uiState.value
        assertEquals(SmartImportStage.REVIEW, state.stage)
        assertEquals(SmartImportMode.RECEIPT, state.mode)
        assertEquals(1, state.candidates.size)
        assertEquals(15.50, state.candidates[0].transaction.amount, 0.001)
        assertEquals("Padaria Pao Quente", state.candidates[0].transaction.description)
        assertTrue(state.candidates[0].isExpense)
        assertTrue(state.canItemize)
        assertFalse(state.itemized)

        model.viewModelScope.cancel()
    }

    @Test
    fun onItemizedToggled_swapsBetweenTheTotalAndTheItems() = runTest {
        val model = receiptViewModel()
        pickReceipt(model)

        model.onItemizedToggled()

        val itemized = model.uiState.value
        assertTrue(itemized.itemized)
        assertEquals(2, itemized.candidates.size)
        assertEquals("PAO FRANCES", itemized.candidates[0].transaction.description)
        assertEquals(9.00, itemized.candidates[0].transaction.amount, 0.001)

        model.onItemizedToggled()

        assertFalse(model.uiState.value.itemized)
        assertEquals(1, model.uiState.value.candidates.size)

        model.viewModelScope.cancel()
    }

    @Test
    fun confirmImport_afterAReceipt_writesTheTotalWithTheGuessedCategory() = runTest {
        whenever(importRepository.insertEntries(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            invocation.getArgument<List<ImportedEntry>>(0)
        }
        whenever(categoryRepository.getAllSync()).thenReturn(emptyList())

        val model = receiptViewModel()
        pickReceipt(model)
        model.confirmImport()
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = argumentCaptor<List<ImportedEntry>>()
        verify(importRepository).insertEntries(captor.capture())
        val written = captor.firstValue
        assertEquals(1, written.size)
        assertEquals(15.50, written[0].amount, 0.001)
        assertEquals("Alimentacao", written[0].category)
        assertEquals("nota.jpg", written[0].fileName)
        assertTrue(written[0].isExpense)

        model.viewModelScope.cancel()
    }

    @Test
    fun onReceiptPicked_withAFileThatIsNotAnImage_reportsAnError() = runTest {
        val model = receiptViewModel()

        model.onReceiptPicked(InMemoryPlatformFile("nota.csv", statementCsv))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = model.uiState.value
        assertEquals(SmartImportStage.IDLE, state.stage)
        assertNotNull(state.error)
        assertTrue(state.candidates.isEmpty())

        model.viewModelScope.cancel()
    }

    @Test
    fun onFilePicked_pdf_reportsAnErrorInsteadOfReadingIt() = runTest {
        viewModel.onFilePicked(InMemoryPlatformFile("extrato.pdf", byteArrayOf(1, 2, 3)))
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
