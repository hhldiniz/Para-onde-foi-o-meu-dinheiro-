package com.hhldiniz.praondefoiomeudinheiro.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The handful of capabilities the shared code needs that Kotlin's common
 * standard library does not provide. Everything else — parsing, aggregation,
 * ViewModels, the whole Compose UI — lives in `commonMain` and is compiled
 * unchanged for both Android and iOS.
 */

/** Wall-clock time in milliseconds since the Unix epoch. */
expect fun currentTimeMillis(): Long

/**
 * Offset of the device's current time zone from UTC, in milliseconds, as it
 * applies at [epochMillis] (so daylight-saving transitions are respected).
 */
expect fun timeZoneOffsetMillis(epochMillis: Long): Int

/** ISO 3166 region code of the device's locale ("BR", "AR", ...); empty when unknown. */
expect fun currentRegionCode(): String

/**
 * Dispatcher for blocking I/O. `Dispatchers.IO` is declared per-platform in
 * kotlinx-coroutines and is not visible from common code, so it is bridged here.
 */
expect val ioDispatcher: CoroutineDispatcher
