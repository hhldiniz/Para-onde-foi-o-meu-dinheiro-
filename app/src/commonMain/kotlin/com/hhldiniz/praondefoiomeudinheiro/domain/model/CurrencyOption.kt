package com.hhldiniz.praondefoiomeudinheiro.domain.model

/**
 * Supported currency options with their ISO code, symbol, and BCP 47 locale
 * tag used for number formatting on each platform.
 */
enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val localeTag: String,
) {
    BRL("BRL", "R$", "pt-BR"),
    USD("USD", "$", "en-US"),
    EUR("EUR", "€", "de-DE"),
    GBP("GBP", "£", "en-GB"),
    ARS("ARS", "$", "es-AR");

    companion object {
        /**
         * Attempts to detect the currency from a raw amount string by looking
         * for symbol prefixes, ISO codes, or number-format patterns (e.g.
         * comma as decimal separator suggesting ARS/BRL vs dot suggesting USD).
         */
        fun fromAmountString(amount: String): CurrencyOption? {
            val trimmed = amount.trim()
            return when {
                trimmed.contains("R\$") -> BRL
                trimmed.contains("€") -> EUR
                trimmed.contains("£") -> GBP
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
