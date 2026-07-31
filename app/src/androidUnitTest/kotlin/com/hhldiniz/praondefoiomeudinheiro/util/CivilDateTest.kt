package com.hhldiniz.praondefoiomeudinheiro.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The calendar maths that replaced `java.time` in common code. `java.time` is
 * still available in this JVM test run, so it doubles as the oracle.
 */
class CivilDateTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    @Test
    fun daysFromCivil_matchesJavaTime() {
        listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(1969, 12, 31),
            LocalDate.of(2000, 2, 29),
            LocalDate.of(2024, 3, 15),
            LocalDate.of(1900, 3, 1),
            LocalDate.of(2100, 12, 31),
        ).forEach { date ->
            assertEquals(
                "days for $date",
                date.toEpochDay(),
                daysFromCivil(date.year, date.monthValue, date.dayOfMonth),
            )
        }
    }

    @Test
    fun civilFromDays_roundTrips() {
        var day = -25_000L
        while (day <= 25_000L) {
            val date = civilFromDays(day)
            assertEquals(day, daysFromCivil(date.year, date.month, date.day))
            day += 137
        }
    }

    @Test
    fun localDateOf_matchesJavaTime() {
        listOf(0L, 1_700_000_000_000L, 1_000L, 4_102_444_800_000L).forEach { millis ->
            val expected = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            val actual = localDateOf(millis)
            assertEquals(CivilDate(expected.year, expected.monthValue, expected.dayOfMonth), actual)
        }
    }

    @Test
    fun startOfDayMillis_matchesJavaTime() {
        listOf(
            Triple(2024, 3, 15),
            Triple(2026, 1, 1),
            Triple(1999, 12, 31),
        ).forEach { (year, month, day) ->
            val expected = LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()
            assertEquals("$year-$month-$day", expected, startOfDayMillis(year, month, day))
        }
    }

    @Test
    fun isoWeekOfYear_matchesJavaTime() {
        val weekFields = java.time.temporal.WeekFields.ISO
        var date = LocalDate.of(2023, 12, 25)
        repeat(60) {
            val millis = date.atStartOfDay(zone).toInstant().toEpochMilli()
            assertEquals(
                "week for $date",
                date.get(weekFields.weekOfWeekBasedYear()),
                isoWeekOfYear(millis),
            )
            date = date.plusDays(11)
        }
    }

    @Test
    fun minusMonths_clampsToShorterMonth() {
        val may31 = startOfDayMillis(2024, 5, 31)
        assertEquals(CivilDate(2024, 2, 29), localDateOf(minusMonths(may31, 3)))
    }

    @Test
    fun minusMonths_crossesYearBoundary() {
        val jan15 = startOfDayMillis(2024, 1, 15)
        assertEquals(CivilDate(2023, 10, 15), localDateOf(minusMonths(jan15, 3)))
    }

    @Test
    fun formatDayMonthYear_padsToTwoDigits() {
        assertEquals("05/03/2024", formatDayMonthYear(startOfDayMillis(2024, 3, 5)))
        assertEquals("31/12/1999", formatDayMonthYear(startOfDayMillis(1999, 12, 31)))
    }

    @Test
    fun daysInMonth_handlesLeapYears() {
        assertEquals(29, daysInMonth(2024, 2))
        assertEquals(28, daysInMonth(2023, 2))
        assertEquals(29, daysInMonth(2000, 2))
        assertEquals(28, daysInMonth(1900, 2))
        assertEquals(31, daysInMonth(2024, 1))
        assertEquals(30, daysInMonth(2024, 4))
    }
}
