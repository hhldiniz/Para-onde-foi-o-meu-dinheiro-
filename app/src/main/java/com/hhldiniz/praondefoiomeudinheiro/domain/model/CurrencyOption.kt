package com.hhldiniz.praondefoiomeudinheiro.domain.model

import java.util.Locale

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val locale: Locale,
) {
    BRL("BRL", "R$", Locale.forLanguageTag("pt-BR")),
    USD("USD", "$", Locale.forLanguageTag("en-US")),
    EUR("EUR", "\u20AC", Locale.forLanguageTag("de-DE")),
    GBP("GBP", "\u00A3", Locale.forLanguageTag("en-GB")),
    ARS("ARS", "$", Locale.forLanguageTag("es-AR"));

    companion object {
        fun fromAmountString(amount: String): CurrencyOption? {
            val trimmed = amount.trim()
            return when {
                trimmed.contains("R\$") -> BRL
                trimmed.contains("\u20AC") -> EUR
                trimmed.contains("\u00A3") -> GBP
                trimmed.contains("\$") -> {
                    val cleaned = trimmed.replace("\$", "").trim()
                    if (cleaned.contains(",") && cleaned.contains(".")) {
                        val dotLast = cleaned.lastIndexOf('.')
                        val commaLast = cleaned.lastIndexOf(',')
                        if (commaLast > dotLast) ARS else USD
                    } else if (cleaned.contains(",")) {
                        ARS
                    } else {
                        USD
                    }
                }
                trimmed.uppercase().contains("BRL") -> BRL
                trimmed.uppercase().contains("USD") -> USD
                trimmed.uppercase().contains("EUR") -> EUR
                trimmed.uppercase().contains("GBP") -> GBP
                trimmed.uppercase().contains("ARS") -> ARS
                else -> null
            }
        }
    }
}
