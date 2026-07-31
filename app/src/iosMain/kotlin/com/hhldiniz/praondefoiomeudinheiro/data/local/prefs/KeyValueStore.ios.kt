package com.hhldiniz.praondefoiomeudinheiro.data.local.prefs

import platform.Foundation.NSUserDefaults

/**
 * [KeyValueStore] backed by NSUserDefaults. The store [name] becomes the suite
 * name, mirroring the separate SharedPreferences files used on Android.
 */
class UserDefaultsKeyValueStore(name: String) : KeyValueStore {

    private val defaults = NSUserDefaults(suiteName = name)

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.doubleForKey(key)

    override fun putDouble(key: String, value: Double) {
        defaults.setDouble(value, key)
    }
}

actual fun createKeyValueStore(name: String): KeyValueStore = UserDefaultsKeyValueStore(name)
