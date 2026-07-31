package com.hhldiniz.praondefoiomeudinheiro.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.objectForKey
import platform.Foundation.secondsFromGMTForDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun timeZoneOffsetMillis(epochMillis: Long): Int {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return (NSTimeZone.localTimeZone.secondsFromGMTForDate(date) * 1000L).toInt()
}

actual fun currentRegionCode(): String =
    NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String ?: ""

// Kotlin/Native has no dedicated unbounded I/O pool; Default is the standard
// choice for the file and SQLite work this app does off the main thread.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
