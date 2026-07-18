package com.hhldiniz.praondefoiomeudinheiro.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that broadcasts when the user explicitly clears all app data from
 * the settings screen, so that screens showing data can avoid falling back to
 * mocked values and instead display zeroed fields.
 */
object DataClearedHolder {
    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    fun markCleared() {
        _cleared.value = true
    }

    fun reset() {
        _cleared.value = false
    }
}
