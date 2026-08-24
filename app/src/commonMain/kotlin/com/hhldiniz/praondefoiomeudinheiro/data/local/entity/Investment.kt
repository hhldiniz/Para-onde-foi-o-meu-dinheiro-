package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import com.hhldiniz.praondefoiomeudinheiro.domain.model.YieldMode
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A single position the user tracks by hand: what was put in
 * ([investedAmount]) and what it is worth today ([currentValue]). There is no
 * quote provider — the current value is whatever the user last typed, which
 * is why [updatedAt] is kept.
 *
 * Room-free so it can be shared with wasmJs; the Room-backed persistence for
 * Android/iOS lives in `roomMain` as `InvestmentRecord`.
 */
@Serializable
data class Investment(
    val id: Long = 0,
    val name: String,
    val type: InvestmentType,
    val institution: String = "",
    val investedAmount: Double,
    val currentValue: Double,
    val dateMillis: Long,
    val notes: String = "",
    // How the position is contracted to pay (fixed income only, see
    // InvestmentType.supportsYield). Descriptive: it records what the paper
    // promises, it does not recompute currentValue — the app quotes no index.
    val yieldMode: YieldMode = YieldMode.NONE,
    val yieldRate: Double? = null,
    // Json skips a property whose value equals its default, and this default
    // is re-evaluated at encode time: a position serialized in the same
    // millisecond it was built would be written without its timestamp and
    // read back with whatever "now" was at load time. Encoding it always is
    // what makes the stored value survive a reload on wasmJs.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @OptIn(ExperimentalSerializationApi::class)
    val updatedAt: Long = currentTimeMillis(),
) {
    /** How much the position is up (or down) in absolute terms. */
    val profit: Double get() = currentValue - investedAmount

    /**
     * [profit] as a percentage of what was invested, or 0 when nothing was
     * invested (a fully donated/bonus position), since there is no meaningful
     * return to divide by.
     */
    val profitPercent: Double
        get() = if (investedAmount == 0.0) 0.0 else profit / investedAmount * 100.0

    /**
     * Whether there is a contracted rate to show. A mode without its rate is
     * incomplete — "IPCA + ?" says nothing — so both halves are required,
     * except for [YieldMode.NONE], which is the absence of one.
     */
    val hasYield: Boolean get() = yieldMode.hasRate && yieldRate != null
}
