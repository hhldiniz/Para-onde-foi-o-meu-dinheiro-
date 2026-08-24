package com.hhldiniz.praondefoiomeudinheiro.presentation.components

import androidx.compose.runtime.Composable
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import com.hhldiniz.praondefoiomeudinheiro.domain.model.YieldMode
import com.hhldiniz.praondefoiomeudinheiro.domain.model.formatYieldRate
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_mode_cdi_percent
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_mode_cdi_plus
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_mode_igpm_plus
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_mode_ipca_plus
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_mode_none
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_mode_prefixed
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_mode_selic_plus
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_value_cdi_percent
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_value_cdi_plus
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_value_igpm_plus
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_value_ipca_plus
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_value_prefixed
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_yield_value_selic_plus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How a [YieldMode] is named in the picker — the shape of the deal without a
 * rate in it ("Percentual do CDI"). The mode is persisted as a stable key
 * (see [YieldMode.key]), so this is the only place that says how one is
 * spelled, mirroring [labelRes] for investment types.
 */
val YieldMode.labelRes: StringResource
    get() = when (this) {
        YieldMode.NONE -> Res.string.investment_yield_mode_none
        YieldMode.PREFIXED -> Res.string.investment_yield_mode_prefixed
        YieldMode.CDI_PERCENT -> Res.string.investment_yield_mode_cdi_percent
        YieldMode.CDI_PLUS -> Res.string.investment_yield_mode_cdi_plus
        YieldMode.IPCA_PLUS -> Res.string.investment_yield_mode_ipca_plus
        YieldMode.IGPM_PLUS -> Res.string.investment_yield_mode_igpm_plus
        YieldMode.SELIC_PLUS -> Res.string.investment_yield_mode_selic_plus
    }

/**
 * The template the mode reads as once it carries a rate: "110% do CDI",
 * "IPCA + 5.5%". [YieldMode.NONE] has none — there is nothing to render — so
 * it is the one entry that returns null.
 */
private val YieldMode.valueTemplateRes: StringResource?
    get() = when (this) {
        YieldMode.NONE -> null
        YieldMode.PREFIXED -> Res.string.investment_yield_value_prefixed
        YieldMode.CDI_PERCENT -> Res.string.investment_yield_value_cdi_percent
        YieldMode.CDI_PLUS -> Res.string.investment_yield_value_cdi_plus
        YieldMode.IPCA_PLUS -> Res.string.investment_yield_value_ipca_plus
        YieldMode.IGPM_PLUS -> Res.string.investment_yield_value_igpm_plus
        YieldMode.SELIC_PLUS -> Res.string.investment_yield_value_selic_plus
    }

@Composable
fun localizedYieldMode(mode: YieldMode): String = stringResource(mode.labelRes)

/**
 * The whole contracted yield of [investment] as one string, or null when it
 * carries none — which is every variable-income position and any fixed-income
 * one the user did not fill in.
 */
@Composable
fun localizedInvestmentYield(investment: Investment): String? {
    val template = investment.yieldMode.valueTemplateRes ?: return null
    val rate = investment.yieldRate ?: return null
    return stringResource(template, formatYieldRate(rate))
}
