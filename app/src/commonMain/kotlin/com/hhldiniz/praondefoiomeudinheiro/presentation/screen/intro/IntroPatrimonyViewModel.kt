package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro

import androidx.lifecycle.ViewModel
import com.hhldiniz.praondefoiomeudinheiro.data.local.PatrimonyHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI state for the patrimony introduction screen. */
data class IntroPatrimonyUiState(
    val amountText: String = "",
    val confirmed: Boolean = false,
)

/**
 * ViewModel for the first onboarding screen, where the user enters the
 * patrimony (net worth) they want to start tracking from. The value is
 * persisted to [PatrimonyHolder] so [com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeViewModel]
 * picks it up as the starting balance.
 */
class IntroPatrimonyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IntroPatrimonyUiState())
    val uiState: StateFlow<IntroPatrimonyUiState> = _uiState.asStateFlow()

    fun onAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amountText = amount)
    }

    /** Persists the entered amount (defaulting to 0 when blank/invalid) and signals completion. */
    fun onContinue() {
        val amount = _uiState.value.amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
        PatrimonyHolder.setPatrimony(amount)
        _uiState.value = _uiState.value.copy(confirmed = true)
    }
}
