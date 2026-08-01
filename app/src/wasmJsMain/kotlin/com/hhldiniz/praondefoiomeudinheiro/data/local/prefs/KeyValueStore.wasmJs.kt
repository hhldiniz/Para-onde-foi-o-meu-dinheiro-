package com.hhldiniz.praondefoiomeudinheiro.data.local.prefs

import kotlinx.browser.localStorage

private fun jsConsoleError(message: String): Unit = js("console.error(message)")

/**
 * [KeyValueStore] backed by `localStorage`. The store [name] namespaces keys
 * (`"$name.$key"`), mirroring the separate SharedPreferences files on
 * Android and the NSUserDefaults suites on iOS.
 *
 * `localStorage` can throw where SharedPreferences/NSUserDefaults never do
 * (quota exceeded, private-browsing/storage-partitioning restrictions), so
 * calls are caught and logged rather than left to propagate into whatever
 * coroutine invoked them.
 */
class WebKeyValueStore(private val name: String) : KeyValueStore {

    private fun namespacedKey(key: String) = "$name.$key"

    override fun getString(key: String): String? =
        runCatching { localStorage.getItem(namespacedKey(key)) }
            .onFailure { jsConsoleError("praondefoiomeudinheiro: failed to read $name.$key: ${it.message}") }
            .getOrNull()

    override fun putString(key: String, value: String) {
        runCatching { localStorage.setItem(namespacedKey(key), value) }
            .onFailure { jsConsoleError("praondefoiomeudinheiro: failed to write $name.$key: ${it.message}") }
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        runCatching { localStorage.getItem(namespacedKey(key)) }
            .onFailure { jsConsoleError("praondefoiomeudinheiro: failed to read $name.$key: ${it.message}") }
            .getOrNull()?.toDoubleOrNull() ?: defaultValue

    override fun putDouble(key: String, value: Double) {
        runCatching { localStorage.setItem(namespacedKey(key), value.toString()) }
            .onFailure { jsConsoleError("praondefoiomeudinheiro: failed to write $name.$key: ${it.message}") }
    }
}

actual fun createKeyValueStore(name: String): KeyValueStore = WebKeyValueStore(name)
