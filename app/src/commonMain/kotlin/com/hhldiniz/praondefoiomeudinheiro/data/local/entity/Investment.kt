package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
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
}
