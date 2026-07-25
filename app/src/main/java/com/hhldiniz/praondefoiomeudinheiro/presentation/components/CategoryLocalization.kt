package com.hhldiniz.praondefoiomeudinheiro.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hhldiniz.praondefoiomeudinheiro.R

private val defaultCategoryStringRes = mapOf(
    "Alimentacao" to R.string.category_alimentacao,
    "Transporte" to R.string.category_transporte,
    "Lazer" to R.string.category_lazer,
    "Saude" to R.string.category_saude,
    "Educacao" to R.string.category_educacao,
    "Moradia" to R.string.category_moradia,
    "Salario" to R.string.category_salario,
    "Freelance" to R.string.category_freelance,
    "Investimentos" to R.string.category_investimentos,
    "Outros" to R.string.category_outros,
)

/**
 * Default seeded categories are stored in Portuguese; user-created categories are stored as
 * typed. This resolves the display name for the current locale, leaving user-created names as-is.
 */
@Composable
fun localizedCategoryName(name: String): String {
    val resId = defaultCategoryStringRes[name] ?: return name
    return stringResource(resId)
}
