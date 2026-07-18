package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CurrencyHolder {
    private val _selectedCurrency = MutableStateFlow(CurrencyOption.BRL)
    val selectedCurrency: StateFlow<CurrencyOption> = _selectedCurrency.asStateFlow()

    fun setCurrency(currency: CurrencyOption) {
        _selectedCurrency.value = currency
    }
}
