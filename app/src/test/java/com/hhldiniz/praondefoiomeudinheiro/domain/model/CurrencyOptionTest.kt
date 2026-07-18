package com.hhldiniz.praondefoiomeudinheiro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyOptionTest {

    @Test
    fun detectsBrlBySymbol() {
        assertEquals(CurrencyOption.BRL, CurrencyOption.fromAmountString("R$ 10,00"))
    }

    @Test
    fun detectsEurBySymbol() {
        assertEquals(CurrencyOption.EUR, CurrencyOption.fromAmountString("10,00 €"))
    }

    @Test
    fun detectsGbpBySymbol() {
        assertEquals(CurrencyOption.GBP, CurrencyOption.fromAmountString("£ 10.00"))
    }

    @Test
    fun detectsUsdByCode() {
        assertEquals(CurrencyOption.USD, CurrencyOption.fromAmountString("USD 10.00"))
    }

    @Test
    fun detectsArsByCode() {
        assertEquals(CurrencyOption.ARS, CurrencyOption.fromAmountString("ARS 10,00"))
    }

    @Test
    fun detectsArsByCommaDecimalWithSymbol() {
        assertEquals(CurrencyOption.ARS, CurrencyOption.fromAmountString("$1.234,56"))
    }

    @Test
    fun detectsUsdByDotDecimalWithSymbol() {
        assertEquals(CurrencyOption.USD, CurrencyOption.fromAmountString("$1,234.56"))
    }

    @Test
    fun detectsArsByCommaOnly() {
        assertEquals(CurrencyOption.ARS, CurrencyOption.fromAmountString("$10,00"))
    }

    @Test
    fun detectsUsdByPlainSymbol() {
        assertEquals(CurrencyOption.USD, CurrencyOption.fromAmountString("$10.00"))
    }

    @Test
    fun returnsNullForUnrecognized() {
        assertNull(CurrencyOption.fromAmountString("abc"))
        assertNull(CurrencyOption.fromAmountString(""))
        assertNull(CurrencyOption.fromAmountString("1000"))
    }

    @Test
    fun enumValuesHaveExpectedCodes() {
        assertEquals("BRL", CurrencyOption.BRL.code)
        assertEquals("USD", CurrencyOption.USD.code)
        assertEquals("EUR", CurrencyOption.EUR.code)
        assertEquals("GBP", CurrencyOption.GBP.code)
        assertEquals("ARS", CurrencyOption.ARS.code)
    }

    @Test
    fun enumValuesHaveExpectedSymbols() {
        assertEquals("R$", CurrencyOption.BRL.symbol)
        assertEquals("$", CurrencyOption.USD.symbol)
        assertEquals("€", CurrencyOption.EUR.symbol)
        assertEquals("£", CurrencyOption.GBP.symbol)
    }
}
