package com.hhldiniz.praondefoiomeudinheiro.data.local.web

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
