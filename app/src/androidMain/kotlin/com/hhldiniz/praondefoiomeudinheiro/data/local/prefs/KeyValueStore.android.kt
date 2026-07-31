package com.hhldiniz.praondefoiomeudinheiro.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.hhldiniz.praondefoiomeudinheiro.platform.AndroidAppContext

/** [KeyValueStore] backed by SharedPreferences. */
class SharedPreferencesKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        // Stored as a float historically; keep reading/writing that type so
        // values saved by the Android-only version survive the upgrade.
        prefs.getFloat(key, defaultValue.toFloat()).toDouble()

    override fun putDouble(key: String, value: Double) {
        prefs.edit { putFloat(key, value.toFloat()) }
    }
}

actual fun createKeyValueStore(name: String): KeyValueStore =
    SharedPreferencesKeyValueStore(
        AndroidAppContext.require().getSharedPreferences(name, Context.MODE_PRIVATE)
    )
