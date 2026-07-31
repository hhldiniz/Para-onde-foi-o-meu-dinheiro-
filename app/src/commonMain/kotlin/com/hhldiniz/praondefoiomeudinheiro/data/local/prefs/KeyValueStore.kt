package com.hhldiniz.praondefoiomeudinheiro.data.local.prefs

/**
 * The tiny slice of key/value persistence the app needs, backed by
 * SharedPreferences on Android and NSUserDefaults on iOS. Keeping it behind
 * an interface lets [com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder]
 * and friends stay in common code (and be unit-tested with a fake).
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getDouble(key: String, defaultValue: Double): Double
    fun putDouble(key: String, value: Double)
}

/** Creates (or opens) the named store for the current platform. */
expect fun createKeyValueStore(name: String): KeyValueStore

/** In-memory [KeyValueStore] for tests and previews. */
class InMemoryKeyValueStore(initial: Map<String, Any> = emptyMap()) : KeyValueStore {
    private val values = initial.toMutableMap()

    override fun getString(key: String): String? = values[key] as? String

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        (values[key] as? Double) ?: defaultValue

    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }
}
