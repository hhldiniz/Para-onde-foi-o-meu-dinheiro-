package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CurrencyHolder {
    private const val PREFS_NAME = "currency_prefs"
    private const val KEY_CURRENCY = "selected_currency"

    private val _selectedCurrency = MutableStateFlow(CurrencyOption.BRL)
    val selectedCurrency: StateFlow<CurrencyOption> = _selectedCurrency.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs?.getString(KEY_CURRENCY, null)
        if (savedCode != null) {
            val currency = CurrencyOption.entries.find { it.code == savedCode }
            if (currency != null) {
                _selectedCurrency.value = currency
            }
        }
    }

    fun setCurrency(currency: CurrencyOption) {
        _selectedCurrency.value = currency
        prefs?.edit()?.putString(KEY_CURRENCY, currency.code)?.apply()
    }
}
