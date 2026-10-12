package com.hhldiniz.praondefoiomeudinheiro.data.local.web

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private fun jsConsoleError(message: String): Unit = js("console.error(message)")

/**
 * Logs a `localStorage` read/write failure to the browser console rather than
 * letting it propagate. `localStorage` can throw for reasons Room never does
 * on Android/iOS (quota exceeded, private-browsing or storage-partitioning
 * restrictions), and an uncaught throw here would otherwise kill whatever
 * `viewModelScope.launch` coroutine called into the DAO mid-flight.
 */
internal fun logStorageError(action: String, error: Throwable) {
    jsConsoleError("praondefoiomeudinheiro: failed to $action: ${error.message}")
}

/**
 * One "table" of the web DAOs: the whole row list held in memory as a
 * [StateFlow] and re-serialized to a single `localStorage` key on every
 * [write]. Read and write failures are logged via [logStorageError] instead
 * of thrown, so a `localStorage` hiccup degrades to in-memory-only
 * persistence for that operation.
 */
internal class LocalStorageTable<T>(
    private val storageKey: String,
    private val name: String,
    private val serializer: KSerializer<List<T>>,
) {
    private val state = MutableStateFlow(load())

    val flow: StateFlow<List<T>> get() = state

    val rows: List<T> get() = state.value

    fun write(rows: List<T>) {
        state.value = rows
        runCatching { localStorage.setItem(storageKey, Json.encodeToString(serializer, rows)) }
            .onFailure { logStorageError("persist $name", it) }
    }

    private fun load(): List<T> {
        val raw = runCatching { localStorage.getItem(storageKey) }
            .onFailure { logStorageError("load $name", it) }
            .getOrNull() ?: return emptyList()
        return runCatching { Json.decodeFromString(serializer, raw) }
            .onFailure { logStorageError("decode $name", it) }
            .getOrDefault(emptyList())
    }
}

internal inline fun <reified T> localStorageTable(storageKey: String, name: String) =
    LocalStorageTable<T>(storageKey, name, serializer())
