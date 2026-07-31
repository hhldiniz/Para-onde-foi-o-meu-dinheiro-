package com.hhldiniz.praondefoiomeudinheiro.util

import com.hhldiniz.praondefoiomeudinheiro.platform.timeZoneOffsetMillis

/**
 * Calendar arithmetic shared by both platforms, replacing the `java.time` /
 * `java.util.Calendar` usage of the Android-only version. Only the time-zone
 * offset itself is platform-specific (see [timeZoneOffsetMillis]); the
 * proleptic-Gregorian conversions below are plain integer maths.
 */

private const val MILLIS_PER_DAY = 86_400_000L

/** A date on the proleptic Gregorian calendar, with [month] and [day] 1-based. */
data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

private fun floorDiv(a: Long, b: Long): Long {
    val q = a / b
    return if (a % b != 0L && (a xor b) < 0L) q - 1 else q
}

private fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b

/**
 * Days since 1970-01-01 for a civil date (Howard Hinnant's `days_from_civil`).
 * Valid for any year representable in [Int].
 */
fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yearOfEra = y - era * 400
    val mp = (if (month > 2) month - 3 else month + 9).toLong()
    val dayOfYear = (153 * mp + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097 + dayOfEra - 719_468
}

/** Inverse of [daysFromCivil] (Hinnant's `civil_from_days`). */
fun civilFromDays(days: Long): CivilDate {
    val z = days + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val dayOfEra = z - era * 146_097
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val y = yearOfEra + era * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val mp = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    return CivilDate(
        year = (if (month <= 2) y + 1 else y).toInt(),
        month = month.toInt(),
        day = day.toInt(),
    )
}

/** Number of days in [month] of [year], honouring leap years. */
fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 0
}

fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

/** The local calendar date the given epoch timestamp falls on. */
fun localDateOf(epochMillis: Long): CivilDate {
    val local = epochMillis + timeZoneOffsetMillis(epochMillis)
    return civilFromDays(floorDiv(local, MILLIS_PER_DAY))
}

/**
 * Epoch millis of local midnight on the given date. The offset is resolved in
 * two passes so dates on the far side of a DST change still map to their own
 * midnight rather than the one implied by today's offset.
 */
fun startOfDayMillis(year: Int, month: Int, day: Int): Long {
    val asUtc = daysFromCivil(year, month, day) * MILLIS_PER_DAY
    val firstGuess = asUtc - timeZoneOffsetMillis(asUtc)
    return asUtc - timeZoneOffsetMillis(firstGuess)
}

/** ISO-8601 week number (weeks start on Monday; week 1 contains the first Thursday). */
fun isoWeekOfYear(epochMillis: Long): Int {
    val date = localDateOf(epochMillis)
    val days = daysFromCivil(date.year, date.month, date.day)
    // 1970-01-01 was a Thursday, so `days + 3` makes Monday the 0 of the cycle.
    val dayOfWeek = floorMod(days + 3, 7)
    val thursdayOfThisWeek = days - dayOfWeek + 3
    val isoYear = civilFromDays(thursdayOfThisWeek).year
    val jan1 = daysFromCivil(isoYear, 1, 1)
    return ((thursdayOfThisWeek - jan1) / 7 + 1).toInt()
}

/**
 * Shifts [epochMillis] back by [months] calendar months, clamping the day to
 * the target month's length (the behaviour of `Calendar.add(MONTH, -n)`).
 */
fun minusMonths(epochMillis: Long, months: Int): Long {
    val date = localDateOf(epochMillis)
    val zeroBased = date.year * 12 + (date.month - 1) - months
    val year = floorDiv(zeroBased.toLong(), 12L).toInt()
    val month = floorMod(zeroBased.toLong(), 12L).toInt() + 1
    val day = minOf(date.day, daysInMonth(year, month))
    val timeOfDay = epochMillis - startOfDayMillis(date.year, date.month, date.day)
    return startOfDayMillis(year, month, day) + timeOfDay
}

/** Formats [epochMillis] as `dd/MM/yyyy` in the device's time zone. */
fun formatDayMonthYear(epochMillis: Long): String {
    val date = localDateOf(epochMillis)
    return "${pad2(date.day)}/${pad2(date.month)}/${date.year.toString().padStart(4, '0')}"
}

private fun pad2(value: Int): String = if (value < 10) "0$value" else value.toString()
