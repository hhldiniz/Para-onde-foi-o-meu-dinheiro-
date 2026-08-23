package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that persists the user's starting patrimony (net worth) in a
 * [KeyValueStore] and exposes it as a [StateFlow] for reactive UIs.
 */
object PatrimonyHolder {
    const val PREFS_NAME = "patrimony_prefs"
    private const val KEY_PATRIMONY = "patrimony"

    private val _patrimony = MutableStateFlow(0.0)
    val patrimony: StateFlow<Double> = _patrimony.asStateFlow()

    private var store: KeyValueStore? = null

    /** Initialises the holder from persisted storage; should be called once at app start. */
    fun init(store: KeyValueStore) {
        this.store = store
        _patrimony.value = store.getDouble(KEY_PATRIMONY, 0.0)
    }

    /** Persists the given [value] and updates the reactive state. */
    fun setPatrimony(value: Double) {
        _patrimony.value = value
        store?.putDouble(KEY_PATRIMONY, value)
    }

    /**
     * Clears the stored patrimony back to zero, so onboarding asks for it
     * again. Used when the user clears all app data.
     */
    fun reset() {
        setPatrimony(0.0)
    }
}
