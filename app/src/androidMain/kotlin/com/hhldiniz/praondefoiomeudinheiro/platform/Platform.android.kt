package com.hhldiniz.praondefoiomeudinheiro.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import java.util.TimeZone

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun timeZoneOffsetMillis(epochMillis: Long): Int =
    TimeZone.getDefault().getOffset(epochMillis)

actual fun currentRegionCode(): String = Locale.getDefault().country

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
