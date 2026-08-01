package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.OnboardingHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.defaultCategories
import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.InMemoryKeyValueStore
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/** Unit tests for [IntroCategoriesViewModel]. */
@OptIn(ExperimentalCoroutinesApi::class)
class IntroCategoriesViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: IntroCategoriesViewModel

    private val defaultNames = defaultCategories().map { it.name }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = mock()
        viewModel = IntroCategoriesViewModel(categoryRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        OnboardingHolder.init(InMemoryKeyValueStore())
    }

    @Test
    fun initialState_hasAllDefaultsAvailableAndSelected() {
        val state = viewModel.uiState.value
        assertEquals(defaultNames, state.availableCategories)
        assertEquals(defaultNames.toSet(), state.selectedCategories)
        assertFalse(state.confirmed)
    }

    @Test
    fun onCategoryToggled_deselectsASelectedCategory() {
        viewModel.onCategoryToggled("Lazer")
        assertFalse("Lazer" in viewModel.uiState.value.selectedCategories)
    }

    @Test
    fun onCategoryToggled_reselectsADeselectedCategory() {
        viewModel.onCategoryToggled("Lazer")
        viewModel.onCategoryToggled("Lazer")
        assertTrue("Lazer" in viewModel.uiState.value.selectedCategories)
    }

    @Test
    fun onNewCategoryTextChanged_updatesText() {
        viewModel.onNewCategoryTextChanged("Pets")
        assertEquals("Pets", viewModel.uiState.value.newCategoryText)
    }

    @Test
    fun addCustomCategory_addsToAvailableAndSelected() {
        viewModel.onNewCategoryTextChanged("Pets")
        viewModel.addCustomCategory()

        val state = viewModel.uiState.value
        assertTrue("Pets" in state.availableCategories)
        assertTrue("Pets" in state.selectedCategories)
        assertEquals("", state.newCategoryText)
    }

    @Test
    fun addCustomCategory_blankText_doesNothing() {
        viewModel.addCustomCategory()
        assertEquals(defaultNames, viewModel.uiState.value.availableCategories)
    }

    @Test
    fun addCustomCategory_duplicateName_doesNotDuplicate() {
        viewModel.onNewCategoryTextChanged("Lazer")
        viewModel.addCustomCategory()
        assertEquals(1, viewModel.uiState.value.availableCategories.count { it == "Lazer" })
    }

    @Test
    fun onContinue_removesDeselectedDefaults() = runTest {
        viewModel.onCategoryToggled("Freelance")
        viewModel.onContinue()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(categoryRepository).deleteByName("Freelance")
    }

    @Test
    fun onContinue_doesNotRemoveSelectedDefaults() = runTest {
        viewModel.onContinue()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(categoryRepository, never()).deleteByName("Alimentacao")
    }

    @Test
    fun onContinue_insertsSelectedCategories() = runTest {
        viewModel.onContinue()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(categoryRepository).insertAll(defaultNames)
    }

    @Test
    fun onContinue_setsConfirmedTrue() = runTest {
        viewModel.onContinue()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.confirmed)
    }

    @Test
    fun onContinue_marksOnboardingCompleted() = runTest {
        OnboardingHolder.init(InMemoryKeyValueStore())

        viewModel.onContinue()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(OnboardingHolder.completed.value)
    }
}
