package com.hhldiniz.praondefoiomeudinheiro.data.local.prefs

import kotlinx.browser.localStorage

/**
 * [KeyValueStore] backed by `localStorage`. The store [name] namespaces keys
 * (`"$name.$key"`), mirroring the separate SharedPreferences files on
 * Android and the NSUserDefaults suites on iOS.
 */
class WebKeyValueStore(private val name: String) : KeyValueStore {

    private fun namespacedKey(key: String) = "$name.$key"

    override fun getString(key: String): String? = localStorage.getItem(namespacedKey(key))

    override fun putString(key: String, value: String) {
        localStorage.setItem(namespacedKey(key), value)
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        localStorage.getItem(namespacedKey(key))?.toDoubleOrNull() ?: defaultValue

    override fun putDouble(key: String, value: Double) {
        localStorage.setItem(namespacedKey(key), value.toString())
    }
}

actual fun createKeyValueStore(name: String): KeyValueStore = WebKeyValueStore(name)
