package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import kotlin.js.JsFun

@JsFun(
    "(value, localeTag, currencyCode) => new Intl.NumberFormat(localeTag, " +
        "{ style: 'currency', currency: currencyCode }).format(value)"
)
private external fun jsFormatCurrency(value: Double, localeTag: String, currencyCode: String): String

actual fun currencyFormatter(currency: CurrencyOption): CurrencyFormatter =
    CurrencyFormatter { value -> jsFormatCurrency(value, currency.localeTag, currency.code) }
