package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import java.text.NumberFormat
import java.util.Locale

actual fun currencyFormatter(currency: CurrencyOption): CurrencyFormatter {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(currency.localeTag))
    return CurrencyFormatter { value -> format.format(value) }
}
