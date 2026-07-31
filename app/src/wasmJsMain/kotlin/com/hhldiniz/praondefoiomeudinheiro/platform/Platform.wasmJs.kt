package com.hhldiniz.praondefoiomeudinheiro.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

private fun jsNow(): Double = js("Date.now()")

private fun jsTimezoneOffsetMinutes(epochMillis: Double): Double =
    js("new Date(epochMillis).getTimezoneOffset()")

private fun jsNavigatorLanguage(): String = js("navigator.language || ''")

actual fun currentTimeMillis(): Long = jsNow().toLong()

actual fun timeZoneOffsetMillis(epochMillis: Long): Int =
    // JS's getTimezoneOffset() is UTC-minus-local in minutes, the opposite
    // sign convention from java.util.TimeZone/NSTimeZone's local-minus-UTC.
    (-jsTimezoneOffsetMinutes(epochMillis.toDouble()) * 60_000).toInt()

actual fun currentRegionCode(): String {
    val language = jsNavigatorLanguage()
    val dash = language.indexOf('-')
    return if (dash >= 0) language.substring(dash + 1).uppercase() else ""
}

// wasmJs runs single-threaded in the browser; Default is the standard
// choice, same reasoning as the iOS actual's comment.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
