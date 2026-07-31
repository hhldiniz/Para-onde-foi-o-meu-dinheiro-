package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.KeyValueStore
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that persists the user's preferred [CurrencyOption] in a
 * [KeyValueStore] and exposes it as a [StateFlow] for reactive UIs.
 */
object CurrencyHolder {
    const val PREFS_NAME = "currency_prefs"
    private const val KEY_CURRENCY = "selected_currency"

    private val _selectedCurrency = MutableStateFlow(CurrencyOption.BRL)
    val selectedCurrency: StateFlow<CurrencyOption> = _selectedCurrency.asStateFlow()

    private var store: KeyValueStore? = null

    /** Initialises the holder from persisted storage; should be called once at app start. */
    fun init(store: KeyValueStore) {
        this.store = store
        val savedCode = store.getString(KEY_CURRENCY)
        if (savedCode != null) {
            val currency = CurrencyOption.entries.find { it.code == savedCode }
            if (currency != null) {
                _selectedCurrency.value = currency
            }
        }
    }

    /** Persists the given [currency] and updates the reactive state. */
    fun setCurrency(currency: CurrencyOption) {
        _selectedCurrency.value = currency
        store?.putString(KEY_CURRENCY, currency.code)
    }
}
