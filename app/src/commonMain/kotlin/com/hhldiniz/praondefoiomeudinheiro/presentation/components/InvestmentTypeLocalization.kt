package com.hhldiniz.praondefoiomeudinheiro.presentation.components

import androidx.compose.runtime.Composable
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentClass
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_class_fixed_income
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_class_other
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_class_variable_income
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_bdr
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_cdb
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_commodities
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_cri_cra
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_crypto
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_debenture
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_etf
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_fixed_income_fund
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_foreign_currency
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_foreign_stocks
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_lci_lca
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_multimarket_fund
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_other
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_pension
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_real_estate
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_real_estate_fund
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_savings
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_stocks
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_type_treasury

/**
 * Display name for an [InvestmentType]. The type itself is persisted as a
 * stable key (see [InvestmentType.key]), so this is the only place that says
 * how one is spelled — mirroring how [localizedCategoryName] handles the
 * seeded spending categories.
 */
val InvestmentType.labelRes: StringResource
    get() = when (this) {
        InvestmentType.SAVINGS -> Res.string.investment_type_savings
        InvestmentType.TREASURY -> Res.string.investment_type_treasury
        InvestmentType.CDB -> Res.string.investment_type_cdb
        InvestmentType.LCI_LCA -> Res.string.investment_type_lci_lca
        InvestmentType.CRI_CRA -> Res.string.investment_type_cri_cra
        InvestmentType.DEBENTURE -> Res.string.investment_type_debenture
        InvestmentType.FIXED_INCOME_FUND -> Res.string.investment_type_fixed_income_fund
        InvestmentType.STOCKS -> Res.string.investment_type_stocks
        InvestmentType.REAL_ESTATE_FUND -> Res.string.investment_type_real_estate_fund
        InvestmentType.ETF -> Res.string.investment_type_etf
        InvestmentType.BDR -> Res.string.investment_type_bdr
        InvestmentType.FOREIGN_STOCKS -> Res.string.investment_type_foreign_stocks
        InvestmentType.MULTIMARKET_FUND -> Res.string.investment_type_multimarket_fund
        InvestmentType.CRYPTO -> Res.string.investment_type_crypto
        InvestmentType.COMMODITIES -> Res.string.investment_type_commodities
        InvestmentType.FOREIGN_CURRENCY -> Res.string.investment_type_foreign_currency
        InvestmentType.PENSION -> Res.string.investment_type_pension
        InvestmentType.REAL_ESTATE -> Res.string.investment_type_real_estate
        InvestmentType.OTHER -> Res.string.investment_type_other
    }

/** Display name for the group an investment type is listed under. */
val InvestmentClass.labelRes: StringResource
    get() = when (this) {
        InvestmentClass.FIXED_INCOME -> Res.string.investment_class_fixed_income
        InvestmentClass.VARIABLE_INCOME -> Res.string.investment_class_variable_income
        InvestmentClass.OTHER -> Res.string.investment_class_other
    }

@Composable
fun localizedInvestmentType(type: InvestmentType): String = stringResource(type.labelRes)

@Composable
fun localizedInvestmentClass(assetClass: InvestmentClass): String = stringResource(assetClass.labelRes)
