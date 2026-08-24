package com.hhldiniz.praondefoiomeudinheiro.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The broad asset class an [InvestmentType] belongs to. Only used to group
 * the type picker so a long list stays readable; nothing is persisted from
 * it.
 */
enum class InvestmentClass {
    FIXED_INCOME,
    VARIABLE_INCOME,
    OTHER,
}

/**
 * The kinds of investment the app knows about — the mainstream products of
 * the Brazilian market plus the international ones a retail investor is
 * likely to hold.
 *
 * [key] is what gets persisted (the `type` column on Android/iOS, the JSON
 * value on wasmJs), so it must stay stable even if an entry is renamed or
 * reordered; the display name is resolved per locale in
 * `presentation/components/InvestmentTypeLocalization.kt` instead of living
 * here, keeping this model free of the resource loader.
 */
@Serializable
enum class InvestmentType(val key: String, val assetClass: InvestmentClass) {
    @SerialName("savings")
    SAVINGS("savings", InvestmentClass.FIXED_INCOME),

    @SerialName("treasury")
    TREASURY("treasury", InvestmentClass.FIXED_INCOME),

    @SerialName("cdb")
    CDB("cdb", InvestmentClass.FIXED_INCOME),

    @SerialName("lci_lca")
    LCI_LCA("lci_lca", InvestmentClass.FIXED_INCOME),

    @SerialName("cri_cra")
    CRI_CRA("cri_cra", InvestmentClass.FIXED_INCOME),

    @SerialName("debenture")
    DEBENTURE("debenture", InvestmentClass.FIXED_INCOME),

    @SerialName("fixed_income_fund")
    FIXED_INCOME_FUND("fixed_income_fund", InvestmentClass.FIXED_INCOME),

    @SerialName("stocks")
    STOCKS("stocks", InvestmentClass.VARIABLE_INCOME),

    @SerialName("real_estate_fund")
    REAL_ESTATE_FUND("real_estate_fund", InvestmentClass.VARIABLE_INCOME),

    @SerialName("etf")
    ETF("etf", InvestmentClass.VARIABLE_INCOME),

    @SerialName("bdr")
    BDR("bdr", InvestmentClass.VARIABLE_INCOME),

    @SerialName("foreign_stocks")
    FOREIGN_STOCKS("foreign_stocks", InvestmentClass.VARIABLE_INCOME),

    @SerialName("multimarket_fund")
    MULTIMARKET_FUND("multimarket_fund", InvestmentClass.VARIABLE_INCOME),

    @SerialName("crypto")
    CRYPTO("crypto", InvestmentClass.VARIABLE_INCOME),

    @SerialName("commodities")
    COMMODITIES("commodities", InvestmentClass.VARIABLE_INCOME),

    @SerialName("foreign_currency")
    FOREIGN_CURRENCY("foreign_currency", InvestmentClass.OTHER),

    @SerialName("pension")
    PENSION("pension", InvestmentClass.OTHER),

    @SerialName("real_estate")
    REAL_ESTATE("real_estate", InvestmentClass.OTHER),

    @SerialName("other")
    OTHER("other", InvestmentClass.OTHER);

    /**
     * Whether a position of this type is bought with a contracted rate — the
     * "110% do CDI" a CDB is sold as — and therefore offers the yield fields
     * on the form. Only fixed income is: a share or a fund pays whatever the
     * market gives it, so there is nothing to type in.
     */
    val supportsYield: Boolean get() = assetClass == InvestmentClass.FIXED_INCOME

    companion object {
        /**
         * Resolves a persisted [key] back to its type, falling back to
         * [OTHER] so a row written by a newer version (or a hand-edited
         * `localStorage` value) still shows up instead of failing to load.
         */
        fun fromKey(key: String): InvestmentType =
            entries.firstOrNull { it.key == key } ?: OTHER
    }
}
