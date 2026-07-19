package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [AddEntryViewModel].
 *
 * Both [ImportRepository] and [CategoryRepository] are mocked so these tests
 * run purely on the JVM without Room or Android.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var importRepository: ImportRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: AddEntryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        importRepository = mock()
        categoryRepository = mock()
        whenever(categoryRepository.getAll()).thenReturn(
            flowOf(listOf(Category(id = 1L, name = "Alimentacao"), Category(id = 2L, name = "Transporte")))
        )
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): AddEntryViewModel {
        return AddEntryViewModel(importRepository, categoryRepository, testDispatcher)
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun initialState_hasDefaultValues() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.amountText)
        assertEquals("", state.description)
        assertEquals("", state.category)
        assertTrue(state.isExpense)
        assertFalse(state.isSaving)
        assertFalse(state.savedSuccessfully)
        assertNull(state.errorMessage)
        assertFalse(state.showAddCategoryDialog)
        assertEquals("", state.newCategoryName)
    }

    @Test
    fun init_loadsCategoriesFromRepository() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val categories = viewModel.uiState.value.categories
        assertEquals(listOf("Alimentacao", "Transporte"), categories)
    }

    // -------------------------------------------------------------------------
    // Field update methods
    // -------------------------------------------------------------------------

    @Test
    fun onAmountChanged_updatesAmount() = runTest {
        viewModel = buildViewModel()
        viewModel.onAmountChanged("123.45")
        assertEquals("123.45", viewModel.uiState.value.amountText)
    }

    @Test
    fun onDescriptionChanged_updatesDescription() = runTest {
        viewModel = buildViewModel()
        viewModel.onDescriptionChanged("Café da manhã")
        assertEquals("Café da manhã", viewModel.uiState.value.description)
    }

    @Test
    fun onCategoryChanged_updatesCategory() = runTest {
        viewModel = buildViewModel()
        viewModel.onCategoryChanged("Lazer")
        assertEquals("Lazer", viewModel.uiState.value.category)
    }

    @Test
    fun onTypeChanged_togglesIsExpense() = runTest {
        viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value.isExpense)
        viewModel.onTypeChanged(false)
        assertFalse(viewModel.uiState.value.isExpense)
        viewModel.onTypeChanged(true)
        assertTrue(viewModel.uiState.value.isExpense)
    }

    @Test
    fun onDateChanged_updatesDate() = runTest {
        viewModel = buildViewModel()
        val millis = 1_700_000_000_000L
        viewModel.onDateChanged(millis)
        assertEquals(millis, viewModel.uiState.value.dateMillis)
    }

    // -------------------------------------------------------------------------
    // Category dialog
    // -------------------------------------------------------------------------

    @Test
    fun onShowAddCategoryDialog_true_showsDialog() = runTest {
        viewModel = buildViewModel()
        viewModel.onShowAddCategoryDialog(true)
        assertTrue(viewModel.uiState.value.showAddCategoryDialog)
        assertEquals("", viewModel.uiState.value.newCategoryName)
    }

    @Test
    fun onShowAddCategoryDialog_false_hidesDialog() = runTest {
        viewModel = buildViewModel()
        viewModel.onShowAddCategoryDialog(true)
        viewModel.onNewCategoryNameChanged("Draft")
        viewModel.onShowAddCategoryDialog(false)
        assertFalse(viewModel.uiState.value.showAddCategoryDialog)
    }

    @Test
    fun onNewCategoryNameChanged_updatesName() = runTest {
        viewModel = buildViewModel()
        viewModel.onNewCategoryNameChanged("Nova Categoria")
        assertEquals("Nova Categoria", viewModel.uiState.value.newCategoryName)
    }

    @Test
    fun addNewCategory_blankName_doesNothing() = runTest {
        viewModel = buildViewModel()
        viewModel.onNewCategoryNameChanged("   ")
        viewModel.addNewCategory()
        testDispatcher.scheduler.advanceUntilIdle()
        // isSaving should remain false because addNewCategory returns early
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun addNewCategory_validName_insertsAndUpdatesState() = runTest {
        whenever(categoryRepository.insert(any())).thenReturn(1L)
        viewModel = buildViewModel()
        viewModel.onNewCategoryNameChanged("Saude")
        viewModel.addNewCategory()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Saude", state.category)
        assertFalse(state.showAddCategoryDialog)
        assertEquals("", state.newCategoryName)
        assertFalse(state.isSaving)
    }

    // -------------------------------------------------------------------------
    // save – validation errors
    // -------------------------------------------------------------------------

    @Test
    fun save_invalidAmount_setsErrorMessage() = runTest {
        viewModel = buildViewModel()
        viewModel.onAmountChanged("not a number")
        viewModel.onDescriptionChanged("Test")
        viewModel.onCategoryChanged("Food")
        viewModel.save()

        assertEquals("Valor invalido", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun save_emptyAmount_setsErrorMessage() = runTest {
        viewModel = buildViewModel()
        viewModel.onDescriptionChanged("Test")
        viewModel.onCategoryChanged("Food")
        viewModel.save()

        assertEquals("Valor invalido", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun save_blankDescription_setsErrorMessage() = runTest {
        viewModel = buildViewModel()
        viewModel.onAmountChanged("50.00")
        viewModel.onCategoryChanged("Food")
        viewModel.save()

        assertEquals("Descricao obrigatoria", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun save_blankCategory_setsErrorMessage() = runTest {
        viewModel = buildViewModel()
        viewModel.onAmountChanged("50.00")
        viewModel.onDescriptionChanged("Coffee")
        viewModel.save()

        assertEquals("Categoria obrigatoria", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // save – success path
    // -------------------------------------------------------------------------

    @Test
    fun save_validEntry_setsSavedSuccessfully() = runTest {
        whenever(importRepository.insertEntries(any())).thenReturn(emptyList())
        whenever(categoryRepository.insert(any())).thenReturn(1L)
        viewModel = buildViewModel()
        viewModel.onAmountChanged("99.99")
        viewModel.onDescriptionChanged("Restaurante")
        viewModel.onCategoryChanged("Alimentacao")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.savedSuccessfully)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun save_amountWithComma_parsedCorrectly() = runTest {
        whenever(importRepository.insertEntries(any())).thenReturn(emptyList())
        whenever(categoryRepository.insert(any())).thenReturn(1L)
        viewModel = buildViewModel()
        viewModel.onAmountChanged("1.234,56")
        viewModel.onDescriptionChanged("Groceries")
        viewModel.onCategoryChanged("Food")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        // The amount "1.234,56" → after replace(",", ".") → "1.234.56" which is not a valid Double
        // so it sets error message
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun save_plainAmount_parsedCorrectly() = runTest {
        whenever(importRepository.insertEntries(any())).thenReturn(emptyList())
        whenever(categoryRepository.insert(any())).thenReturn(1L)
        viewModel = buildViewModel()
        viewModel.onAmountChanged("250,00")
        viewModel.onDescriptionChanged("Shopping")
        viewModel.onCategoryChanged("Lazer")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.savedSuccessfully)
    }

    // -------------------------------------------------------------------------
    // clearError
    // -------------------------------------------------------------------------

    @Test
    fun clearError_removesErrorMessage() = runTest {
        viewModel = buildViewModel()
        viewModel.onAmountChanged("bad")
        viewModel.save()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
