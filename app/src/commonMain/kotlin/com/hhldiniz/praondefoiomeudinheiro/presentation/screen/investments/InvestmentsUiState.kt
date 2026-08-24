package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.investments

import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import com.hhldiniz.praondefoiomeudinheiro.domain.model.YieldMode
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import org.jetbrains.compose.resources.StringResource

/** UI state for the investments tab: the portfolio, its totals and the open form (if any). */
data class InvestmentsUiState(
    val investments: List<Investment> = emptyList(),
    val totalInvested: Double = 0.0,
    val totalCurrent: Double = 0.0,
    val allocation: List<TypeAllocation> = emptyList(),
    val selectedCurrency: CurrencyOption = CurrencyOption.BRL,
    val form: InvestmentFormState? = null,
    val isSaving: Boolean = false,
    val pendingDeleteId: Long? = null,
) {
    /** Absolute result of the whole portfolio. */
    val totalProfit: Double get() = totalCurrent - totalInvested

    /** [totalProfit] as a percentage of what was put in; 0 while nothing is tracked. */
    val totalProfitPercent: Double
        get() = if (totalInvested == 0.0) 0.0 else totalProfit / totalInvested * 100.0
}

/** How much of the portfolio (by current value) sits in one [InvestmentType]. */
data class TypeAllocation(
    val type: InvestmentType,
    val value: Double,
)

/**
 * The add/edit form. It is part of the UI state rather than remembered in the
 * composable so a rotation (or the web build's re-composition after a resize)
 * does not drop what the user was typing.
 *
 * [id] is null while adding and carries the edited position's id otherwise —
 * which is the only difference between the two flows.
 */
data class InvestmentFormState(
    val id: Long? = null,
    val name: String = "",
    val type: InvestmentType = InvestmentType.TREASURY,
    val institution: String = "",
    val investedAmountText: String = "",
    val currentValueText: String = "",
    val dateMillis: Long = currentTimeMillis(),
    val notes: String = "",
    val yieldMode: YieldMode = YieldMode.NONE,
    val yieldRateText: String = "",
    val errorMessageRes: StringResource? = null,
) {
    val isEditing: Boolean get() = id != null

    /**
     * Whether the yield fields are offered at all. Only fixed income is
     * bought with a contracted rate, so a stock's form stays short instead of
     * asking what index it follows.
     */
    val showsYieldFields: Boolean get() = type.supportsYield

    /** Whether the rate input goes with the chosen mode ([YieldMode.NONE] takes none). */
    val showsYieldRate: Boolean get() = showsYieldFields && yieldMode.hasRate
}
