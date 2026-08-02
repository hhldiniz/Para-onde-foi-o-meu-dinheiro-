package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.util.localDateOf
import com.hhldiniz.praondefoiomeudinheiro.util.startOfDayMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the shared date/amount parsing both import paths rely on — the
 * spreadsheet importer through [com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeViewModel]
 * and the automatic one through [com.hhldiniz.praondefoiomeudinheiro.data.vision.TransactionFieldClassifier].
 */
class TransactionValueParserTest {

    private fun date(year: Int, month: Int, day: Int) = startOfDayMillis(year, month, day)

    // -- Dates ---------------------------------------------------------------

    @Test
    fun parseDate_slashedDayFirst() {
        assertEquals(date(2024, 3, 15), TransactionValueParser.parseDate("15/03/2024"))
    }

    @Test
    fun parseDate_ambiguousPair_readsDayFirst() {
        assertEquals(date(2024, 2, 1), TransactionValueParser.parseDate("01/02/2024"))
    }

    @Test
    fun parseDate_americanOrder_usedWhenDayFirstIsImpossible() {
        assertEquals(date(2024, 12, 25), TransactionValueParser.parseDate("12/25/2024"))
    }

    @Test
    fun parseDate_isoAndDottedAndDashed() {
        assertEquals(date(2024, 3, 15), TransactionValueParser.parseDate("2024-03-15"))
        assertEquals(date(2024, 3, 15), TransactionValueParser.parseDate("15.03.2024"))
        assertEquals(date(2024, 3, 15), TransactionValueParser.parseDate("15-03-2024"))
    }

    @Test
    fun parseDate_twoDigitYear_landsIn2000s() {
        assertEquals(date(2024, 3, 15), TransactionValueParser.parseDate("15/03/24"))
    }

    @Test
    fun parseDate_isoTimestamp_keepsOnlyTheDate() {
        assertEquals(date(2024, 3, 15), TransactionValueParser.parseDate("2024-03-15T10:30:00"))
    }

    @Test
    fun parseDate_namedMonths_inThreeLanguages() {
        assertEquals(date(2024, 1, 12), TransactionValueParser.parseDate("12 de janeiro de 2024"))
        assertEquals(date(2024, 1, 12), TransactionValueParser.parseDate("Jan 12, 2024"))
        assertEquals(date(2024, 3, 5), TransactionValueParser.parseDate("5 marzo 2024"))
        assertEquals(date(2024, 8, 9), TransactionValueParser.parseDate("9 agosto 2024"))
    }

    @Test
    fun parseDate_withoutYear_usesFallback() {
        assertEquals(date(2023, 5, 12), TransactionValueParser.parseDate("12/05", fallbackYear = 2023))
        assertEquals(date(2023, 3, 12), TransactionValueParser.parseDate("12 mar", fallbackYear = 2023))
    }

    @Test
    fun parseDate_withoutYearAndWithoutFallback_usesCurrentYear() {
        val parsed = TransactionValueParser.parseDate("12/05")
        assertNotNull(parsed)
        assertEquals(localDateOf(System.currentTimeMillis()).year, localDateOf(parsed!!).year)
    }

    @Test
    fun parseDate_rejectsNonDates() {
        assertNull(TransactionValueParser.parseDate(""))
        assertNull(TransactionValueParser.parseDate("Mercado"))
        assertNull(TransactionValueParser.parseDate("13/13/2024"))
        assertNull(TransactionValueParser.parseDate("32/01/2024"))
        assertNull(TransactionValueParser.parseDate("2024"))
    }

    @Test
    fun parseDate_dottedPairWithoutYear_isMoneyNotADate() {
        // "12.05" reads as an amount; a yearless date is written "12/05".
        assertNull(TransactionValueParser.parseDate("12.05"))
        assertEquals(12.05, TransactionValueParser.parseAmount("12.05")!!, 0.001)
    }

    // -- Amounts -------------------------------------------------------------

    @Test
    fun parseAmount_bothDecimalConventions() {
        assertEquals(100.50, TransactionValueParser.parseAmount("100.50")!!, 0.001)
        assertEquals(1234.56, TransactionValueParser.parseAmount("1.234,56")!!, 0.001)
        assertEquals(1234.56, TransactionValueParser.parseAmount("1,234.56")!!, 0.001)
        assertEquals(1234567.0, TransactionValueParser.parseAmount("1.234.567")!!, 0.001)
    }

    @Test
    fun parseAmount_loneGroupOfThree_isThousands() {
        assertEquals(1234.0, TransactionValueParser.parseAmount("1.234")!!, 0.001)
        assertEquals(1234.0, TransactionValueParser.parseAmount("1,234")!!, 0.001)
    }

    @Test
    fun parseAmount_stripsCurrencyAndSpacing() {
        assertEquals(1234.56, TransactionValueParser.parseAmount("R$ 1.234,56")!!, 0.001)
        assertEquals(50.0, TransactionValueParser.parseAmount("US$ 50,00")!!, 0.001)
        assertEquals(1234.56, TransactionValueParser.parseAmount("1 234,56")!!, 0.001)
        assertEquals(99.9, TransactionValueParser.parseAmount("99,90 EUR")!!, 0.001)
    }

    @Test
    fun parseAmount_negativesInEveryNotation() {
        assertEquals(-50.0, TransactionValueParser.parseAmount("-50,00")!!, 0.001)
        assertEquals(-50.0, TransactionValueParser.parseAmount("(50,00)")!!, 0.001)
        assertEquals(-50.0, TransactionValueParser.parseAmount("50,00-")!!, 0.001)
    }

    @Test
    fun parseAmount_rejectsNonNumbers() {
        assertNull(TransactionValueParser.parseAmount(""))
        assertNull(TransactionValueParser.parseAmount("NOT_A_NUMBER"))
        assertNull(TransactionValueParser.parseAmount("15/03/2024"))
    }

    // -- Classification helpers ----------------------------------------------

    @Test
    fun looksLikeMoney_needsAMonetarySignal() {
        assertTrue(TransactionValueParser.looksLikeMoney("1.234,56"))
        assertTrue(TransactionValueParser.looksLikeMoney("R$ 5"))
        assertTrue(TransactionValueParser.looksLikeMoney("-5"))
        // A bare integer is a row number as often as it is money.
        assertFalse(TransactionValueParser.looksLikeMoney("5"))
        assertFalse(TransactionValueParser.looksLikeMoney("15/03/2024"))
        assertFalse(TransactionValueParser.looksLikeMoney("Mercado"))
    }

    @Test
    fun looksLikeNumber_acceptsBareNumbersButNotDates() {
        assertTrue(TransactionValueParser.looksLikeNumber("5"))
        assertTrue(TransactionValueParser.looksLikeNumber("1.234,56"))
        assertFalse(TransactionValueParser.looksLikeNumber("15/03/2024"))
        assertFalse(TransactionValueParser.looksLikeNumber("Mercado"))
    }

    @Test
    fun debitCreditMarker_readsTheTrailingLetter() {
        assertEquals(true, TransactionValueParser.debitCreditMarker("1.234,56 D"))
        assertEquals(false, TransactionValueParser.debitCreditMarker("50,00 C"))
        assertNull(TransactionValueParser.debitCreditMarker("50,00"))
        assertNull(TransactionValueParser.debitCreditMarker("D"))
    }
}
