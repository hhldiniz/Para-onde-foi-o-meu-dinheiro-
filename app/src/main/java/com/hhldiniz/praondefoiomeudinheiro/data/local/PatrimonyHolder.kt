package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that persists the user's starting patrimony (net worth) in
 * SharedPreferences and exposes it as a [StateFlow] for reactive UIs.
 */
object PatrimonyHolder {
    private const val PREFS_NAME = "patrimony_prefs"
    private const val KEY_PATRIMONY = "patrimony"

    private val _patrimony = MutableStateFlow(0.0)
    val patrimony: StateFlow<Double> = _patrimony.asStateFlow()

    private var prefs: SharedPreferences? = null

    /** Initialises the holder from SharedPreferences; should be called once at app start. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _patrimony.value = prefs?.getFloat(KEY_PATRIMONY, 0f)?.toDouble() ?: 0.0
    }

    /** Persists the given [value] and updates the reactive state. */
    fun setPatrimony(value: Double) {
        _patrimony.value = value
        prefs?.edit()?.putFloat(KEY_PATRIMONY, value.toFloat())?.apply()
    }
}
