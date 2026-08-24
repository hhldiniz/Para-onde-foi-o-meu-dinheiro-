package com.hhldiniz.praondefoiomeudinheiro.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.round

/**
 * How a fixed-income position is contracted to pay — the shape of the deal,
 * not what it has earned so far. A CDB is sold as "110% do CDI", a Tesouro
 * IPCA+ as "IPCA + 5,5%" and a prefixed one as "12% a.a.", so the rate the
 * user types means something different in each case and the two have to be
 * stored together.
 *
 * This is purely descriptive: nothing here recalculates a position's current
 * value, which stays whatever the user last typed (the app quotes no index).
 * It is what lets the user remember what a position pays without digging the
 * contract out again.
 *
 * [key] is what gets persisted (the `yield_mode` column on Android/iOS, the
 * JSON value on wasmJs), so it must stay stable even if an entry is renamed
 * or reordered; display names live in
 * `presentation/components/InvestmentYieldLocalization.kt`.
 */
@Serializable
enum class YieldMode(val key: String) {

    /** Nothing was informed — the default, and the only option for a position that has no contracted rate. */
    @SerialName("none")
    NONE("none"),

    /** A rate fixed at purchase, per year: "12% a.a.". */
    @SerialName("prefixed")
    PREFIXED("prefixed"),

    /** A share of the CDI: "110% do CDI". */
    @SerialName("cdi_percent")
    CDI_PERCENT("cdi_percent"),

    /** The CDI plus a spread: "CDI + 1,5%". */
    @SerialName("cdi_plus")
    CDI_PLUS("cdi_plus"),

    /** Inflation (IPCA) plus a real rate: "IPCA + 5,5%". */
    @SerialName("ipca_plus")
    IPCA_PLUS("ipca_plus"),

    /** Inflation (IGP-M) plus a real rate. */
    @SerialName("igpm_plus")
    IGPM_PLUS("igpm_plus"),

    /** The Selic rate plus a spread. */
    @SerialName("selic_plus")
    SELIC_PLUS("selic_plus");

    /** Whether a rate goes with this mode; only [NONE] carries none. */
    val hasRate: Boolean get() = this != NONE

    companion object {
        /**
         * Resolves a persisted [key] back to its mode, falling back to [NONE]
         * so a row written by a newer version (or a hand-edited
         * `localStorage` value) still loads, just without its rate shown.
         */
        fun fromKey(key: String): YieldMode = entries.firstOrNull { it.key == key } ?: NONE
    }
}

/**
 * Renders a contracted rate for display: at most two decimals, trailing
 * zeros dropped, always carrying its percent sign ("110%", "5.5%").
 *
 * The decimal point matches how the rest of the investments tab prints
 * percentages (`signedPercent`) rather than the user's locale — the tab has
 * exactly one convention for a percentage and this keeps it.
 */
fun formatYieldRate(rate: Double): String {
    val rounded = round(rate * 100) / 100
    val text = if (abs(rounded - rounded.toLong()) < 0.005) {
        rounded.toLong().toString()
    } else {
        rounded.toString().trimEnd('0').trimEnd('.')
    }
    return "$text%"
}
