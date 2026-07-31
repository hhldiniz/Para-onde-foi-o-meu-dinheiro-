package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption

private fun jsFormatCurrency(value: Double, localeTag: String, currencyCode: String): String =
    js("new Intl.NumberFormat(localeTag, { style: 'currency', currency: currencyCode }).format(value)")

actual fun currencyFormatter(currency: CurrencyOption): CurrencyFormatter =
    CurrencyFormatter { value -> jsFormatCurrency(value, currency.localeTag, currency.code) }
