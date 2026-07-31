package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption

/** Formats an amount as localized currency text, e.g. `R$ 1.234,56`. */
fun interface CurrencyFormatter {
    fun format(value: Double): String
}

/**
 * Builds a formatter for [currency] using the platform's own number
 * formatting (`java.text.NumberFormat` / `NSNumberFormatter`), so amounts read
 * the way users of each OS expect.
 */
expect fun currencyFormatter(currency: CurrencyOption): CurrencyFormatter
