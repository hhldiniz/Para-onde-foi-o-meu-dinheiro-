package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.numberWithDouble

actual fun currencyFormatter(currency: CurrencyOption): CurrencyFormatter {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
        // NSLocale identifiers use underscores ("pt_BR") rather than BCP 47 hyphens.
        locale = NSLocale(localeIdentifier = currency.localeTag.replace('-', '_'))
        currencyCode = currency.code
    }
    return CurrencyFormatter { value ->
        formatter.stringFromNumber(NSNumber.numberWithDouble(value)) ?: value.toString()
    }
}
