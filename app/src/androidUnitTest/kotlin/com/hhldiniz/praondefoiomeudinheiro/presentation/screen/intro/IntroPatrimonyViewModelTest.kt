package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hhldiniz.praondefoiomeudinheiro.data.local.PatrimonyHolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Unit tests for [IntroPatrimonyViewModel]. */
class IntroPatrimonyViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private lateinit var viewModel: IntroPatrimonyViewModel

    @Before
    fun setUp() {
        viewModel = IntroPatrimonyViewModel()
    }

    @After
    fun tearDown() {
        PatrimonyHolder.setPatrimony(0.0)
    }

    @Test
    fun initialState_isEmptyAndNotConfirmed() {
        val state = viewModel.uiState.value
        assertEquals("", state.amountText)
        assertFalse(state.confirmed)
    }

    @Test
    fun onAmountChanged_updatesAmountText() {
        viewModel.onAmountChanged("1500")
        assertEquals("1500", viewModel.uiState.value.amountText)
    }

    @Test
    fun onContinue_persistsParsedAmount() {
        viewModel.onAmountChanged("2500,50")
        viewModel.onContinue()
        assertEquals(2500.50, PatrimonyHolder.patrimony.value, 0.001)
    }

    @Test
    fun onContinue_setsConfirmedTrue() {
        viewModel.onAmountChanged("100")
        viewModel.onContinue()
        assertTrue(viewModel.uiState.value.confirmed)
    }

    @Test
    fun onContinue_blankAmount_persistsZero() {
        viewModel.onContinue()
        assertEquals(0.0, PatrimonyHolder.patrimony.value, 0.0)
        assertTrue(viewModel.uiState.value.confirmed)
    }

    @Test
    fun onContinue_invalidAmount_persistsZero() {
        viewModel.onAmountChanged("abc")
        viewModel.onContinue()
        assertEquals(0.0, PatrimonyHolder.patrimony.value, 0.0)
    }
}
