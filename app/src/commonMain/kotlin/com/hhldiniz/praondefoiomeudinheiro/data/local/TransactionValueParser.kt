package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import com.hhldiniz.praondefoiomeudinheiro.util.daysInMonth
import com.hhldiniz.praondefoiomeudinheiro.util.localDateOf
import com.hhldiniz.praondefoiomeudinheiro.util.normalizeForMatching
import com.hhldiniz.praondefoiomeudinheiro.util.startOfDayMillis

/**
 * Format-agnostic parsing of the two values every import ultimately needs: a
 * date and a monetary amount.
 *
 * The spreadsheet importer used to accept only the handful of shapes the app's
 * own export produces. The automatic importer
 * ([com.hhldiniz.praondefoiomeudinheiro.data.vision.TransactionFieldClassifier])
 * cannot assume any layout at all, and it also needs the *inverse* question
 * answered — "does this cell look like a date/amount at all?" — to decide what
 * each column holds. Both live here so the two import paths agree on what a
 * date and an amount are.
 *
 * Ambiguity is resolved the way the previous spreadsheet parser did: day-first
 * (`01/02/2024` is 1 February) unless that reading is impossible, and
 * two-digit years land in 2000..2099.
 */
object TransactionValueParser {

    /** Currency markers stripped before parsing; also a signal for [looksLikeMoney]. */
    private val CURRENCY_MARKERS = listOf(
        "r\$", "us\$", "u\$s", "ar\$", "brl", "usd", "eur", "ars", "gbp", "clp", "mxn",
        "\$", "€", "£", "¥",
    )

    /** `dd/mm/yyyy`, `yyyy-mm-dd`, `dd.mm.yy`, `dd/mm`, … — separator and field count are free. */
    private val NUMERIC_DATE = Regex("""^(\d{1,4})([/\-.])(\d{1,2})(?:[/\-.](\d{2,4}))?$""")

    /** An ISO timestamp, whose date part is all this app cares about. */
    private val ISO_TIMESTAMP = Regex("""^(\d{4}-\d{2}-\d{2})[T ].*$""")

    /**
     * Portuguese/English/Spanish month names, keyed by their first three
     * letters after accent folding ("março" → "marco" → "mar"), which is
     * enough to separate all twelve in the three languages.
     */
    private val MONTH_PREFIXES: Map<String, Int> = mapOf(
        "jan" to 1, "ene" to 1,
        "fev" to 2, "feb" to 2,
        "mar" to 3,
        "abr" to 4, "apr" to 4,
        "mai" to 5, "may" to 5,
        "jun" to 6,
        "jul" to 7,
        "ago" to 8, "aug" to 8,
        "set" to 9, "sep" to 9,
        "out" to 10, "oct" to 10,
        "nov" to 11,
        "dez" to 12, "dec" to 12, "dic" to 12,
    )

    // Spelled out as a character range instead of `\p{L}` because Kotlin/Wasm
    // compiles Regex down to a JS RegExp without the `u` flag, where Unicode
    // property escapes are not available.
    private val WORD_SPLIT = Regex("""[^0-9A-Za-zÀ-ÖØ-öø-ÿ]+""")
    private val DIGITS_ONLY = Regex("""^\d+$""")

    /** A decimal tail of exactly two digits, the strongest "this is money" signal. */
    private val DECIMAL_TAIL = Regex("""\d[.,]\d{2}(?!\d)""")

    /** Thousands grouping, e.g. `1.234.567` or `1,234`. */
    private val GROUPED_DIGITS = Regex("""\d{1,3}([.,]\d{3})+(?!\d)""")

    /**
     * Parses [text] as a date, returning the start of that day in local time,
     * or null when it is not a date in any recognized shape.
     *
     * Dates written without a year (`12/03`, `12 mar`) — common on bank
     * statements covering a single period — resolve against [fallbackYear],
     * defaulting to the current year.
     */
    fun parseDate(text: String, fallbackYear: Int? = null): Long? {
        val trimmed = ISO_TIMESTAMP.find(text.trim())?.groupValues?.get(1) ?: text.trim()
        if (trimmed.isEmpty()) return null
        return parseNumericDate(trimmed, fallbackYear) ?: parseNamedMonthDate(trimmed, fallbackYear)
    }

    /** True when [text] is a date this parser understands. */
    fun looksLikeDate(text: String): Boolean = parseDate(text) != null

    private fun parseNumericDate(text: String, fallbackYear: Int?): Long? {
        val match = NUMERIC_DATE.matchEntire(text) ?: return null
        val (first, separator, second, third) = match.destructured
        // A dotted pair with no year is money, not a date: "12.05" is twelve
        // and five cents far more often than the 12th of May, while a real
        // yearless date is written "12/05" or "12-05".
        if (third.isEmpty() && separator == ".") return null

        val firstValue = first.toIntOrNull() ?: return null
        val secondValue = second.toIntOrNull() ?: return null
        val thirdValue = third.takeIf { it.isNotEmpty() }?.toIntOrNull()

        // A four-digit leading field can only be a year, so the rest is ISO order.
        if (first.length == 4) {
            val day = thirdValue ?: return null
            return dateOrNull(firstValue, secondValue, day)
        }

        val year = thirdValue?.let { expandYear(it, third.length) } ?: fallbackYear ?: currentYear()
        // Day-first wins when it is a valid date, matching how the spreadsheet
        // importer has always read `01/02/2024`; month-first is the fallback
        // for the American form (`12/25/2024`), which day-first cannot explain.
        return dateOrNull(year, secondValue, firstValue) ?: dateOrNull(year, firstValue, secondValue)
    }

    private fun parseNamedMonthDate(text: String, fallbackYear: Int?): Long? {
        val tokens = text.split(WORD_SPLIT).filter { it.isNotEmpty() }
        if (tokens.size < 2) return null

        var month: Int? = null
        val numbers = mutableListOf<Pair<Int, Int>>() // value to digit count
        for (token in tokens) {
            if (DIGITS_ONLY.matches(token)) {
                token.toIntOrNull()?.let { numbers.add(it to token.length) }
                continue
            }
            if (month == null) {
                month = MONTH_PREFIXES[normalizeForMatching(token).take(3)]
            }
        }
        val resolvedMonth = month ?: return null

        // Whichever number can be a day is the day; the other one (if any) is
        // the year. This keeps "12 jan 2024", "jan 12, 2024" and "12 de
        // janeiro" all working without separate patterns per language.
        val dayIndex = numbers.indexOfFirst { (value, digits) -> digits <= 2 && value in 1..31 }
        if (dayIndex < 0) return null
        val day = numbers[dayIndex].first
        val yearEntry = numbers.filterIndexed { index, _ -> index != dayIndex }.firstOrNull()
        val year = yearEntry?.let { expandYear(it.first, it.second) } ?: fallbackYear ?: currentYear()
        return dateOrNull(year, resolvedMonth, day)
    }

    /** Two-digit years resolve into 2000..2099, as the spreadsheet importer always did. */
    private fun expandYear(value: Int, digitCount: Int): Int = if (digitCount <= 2) 2000 + value else value

    private fun dateOrNull(year: Int, month: Int, day: Int): Long? {
        if (month !in 1..12) return null
        if (day !in 1..daysInMonth(year, month)) return null
        if (year !in 1900..2999) return null
        return startOfDayMillis(year, month, day)
    }

    private fun currentYear(): Int = localDateOf(currentTimeMillis()).year

    /** The year part of [millis] in local time, for callers wiring up [parseDate]'s fallback. */
    fun yearOf(millis: Long): Int = localDateOf(millis).year

    /**
     * Parses [text] as a signed amount, tolerating currency symbols/codes,
     * either decimal convention, thousands grouping, accounting parentheses
     * and a trailing minus.
     *
     * A lone separator followed by exactly three digits (`1.234`) is read as
     * thousands grouping, not as a fractional part — no statement quotes money
     * to three decimal places, and both `pt-BR` and `en-US` files use that
     * shape for "one thousand two hundred thirty-four".
     */
    fun parseAmount(text: String): Double? {
        var cleaned = text.trim().lowercase()
        if (cleaned.isEmpty()) return null

        var negative = false
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            negative = true
            cleaned = cleaned.substring(1, cleaned.length - 1)
        }
        for (marker in CURRENCY_MARKERS) {
            cleaned = cleaned.replace(marker, "")
        }
        // Brazilian statements tag rows "D"/"C" for debit/credit; that is a
        // direction marker, not part of the number (see debitCreditMarker).
        cleaned = cleaned.trim().trimEnd('d', 'c').trim()
        // `isWhitespace()` misses the non-breaking spaces `Intl.NumberFormat`
        // and some banks use as the thousands separator.
        cleaned = cleaned.filterNot { it.isWhitespace() || it == '\u00A0' || it == '\u202F' || it == '\'' }
        if (cleaned.startsWith("-") || cleaned.endsWith("-")) {
            negative = true
            cleaned = cleaned.trim('-')
        }
        cleaned = cleaned.removePrefix("+")
        if (cleaned.isEmpty() || cleaned.none { it.isDigit() }) return null
        if (cleaned.any { !it.isDigit() && it != '.' && it != ',' }) return null

        val normalized = normalizeSeparators(cleaned) ?: return null
        val value = normalized.toDoubleOrNull() ?: return null
        return if (negative) -value else value
    }

    private fun normalizeSeparators(digitsAndSeparators: String): String? {
        val lastDot = digitsAndSeparators.lastIndexOf('.')
        val lastComma = digitsAndSeparators.lastIndexOf(',')
        if (lastDot < 0 && lastComma < 0) return digitsAndSeparators

        val decimalIndex = when {
            // Both conventions present: the rightmost separator is the decimal
            // point and the other one is grouping ("1.234,56" / "1,234.56").
            lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
            else -> {
                val index = maxOf(lastDot, lastComma)
                val tail = digitsAndSeparators.length - index - 1
                // Exactly three digits after a single separator is thousands
                // grouping ("1.234"), except after a leading zero ("0.500"),
                // where a fractional reading is the only sensible one.
                val isGrouping = tail == 3 && index > 0 && digitsAndSeparators.take(index) != "0"
                if (isGrouping) -1 else index
            }
        }

        return buildString(digitsAndSeparators.length) {
            digitsAndSeparators.forEachIndexed { index, c ->
                when {
                    c.isDigit() -> append(c)
                    index == decimalIndex -> append('.')
                    else -> Unit // grouping separator, dropped
                }
            }
        }.takeIf { it.isNotEmpty() && it != "." }
    }

    /**
     * True when [text] reads as *money* rather than as any old number: it
     * carries a currency marker, a two-digit decimal tail, thousands grouping
     * or a sign. Plain integers (row numbers, instalment counts) deliberately
     * fail this test — see [looksLikeNumber] for the weaker one.
     */
    fun looksLikeMoney(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || looksLikeDate(trimmed)) return false
        if (parseAmount(trimmed) == null) return false

        val lower = trimmed.lowercase()
        val hasCurrency = CURRENCY_MARKERS.any { lower.contains(it) }
        val hasSign = trimmed.startsWith("-") || trimmed.startsWith("+") || trimmed.endsWith("-") ||
            (trimmed.startsWith("(") && trimmed.endsWith(")"))
        return hasCurrency || hasSign ||
            DECIMAL_TAIL.containsMatchIn(trimmed) || GROUPED_DIGITS.containsMatchIn(trimmed)
    }

    /** True when [text] is a number of any kind (and not a date). */
    fun looksLikeNumber(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.isNotEmpty() && !looksLikeDate(trimmed) && parseAmount(trimmed) != null
    }

    /**
     * The `D`/`C` (debit/credit) marker some statements append to the amount,
     * or null when there is none. `true` means debit, i.e. an expense.
     */
    fun debitCreditMarker(text: String): Boolean? {
        val trimmed = text.trim().lowercase()
        if (trimmed.length < 2 || !trimmed.any { it.isDigit() }) return null
        return when (trimmed.last()) {
            'd' -> true
            'c' -> false
            else -> null
        }
    }
}
