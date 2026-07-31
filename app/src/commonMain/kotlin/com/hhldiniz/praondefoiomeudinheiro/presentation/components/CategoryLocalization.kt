package com.hhldiniz.praondefoiomeudinheiro.presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.category_alimentacao
import com.hhldiniz.praondefoiomeudinheiro.resources.category_educacao
import com.hhldiniz.praondefoiomeudinheiro.resources.category_freelance
import com.hhldiniz.praondefoiomeudinheiro.resources.category_investimentos
import com.hhldiniz.praondefoiomeudinheiro.resources.category_lazer
import com.hhldiniz.praondefoiomeudinheiro.resources.category_moradia
import com.hhldiniz.praondefoiomeudinheiro.resources.category_outros
import com.hhldiniz.praondefoiomeudinheiro.resources.category_salario
import com.hhldiniz.praondefoiomeudinheiro.resources.category_saude
import com.hhldiniz.praondefoiomeudinheiro.resources.category_transporte

private val defaultCategoryStringRes = mapOf(
    "Alimentacao" to Res.string.category_alimentacao,
    "Transporte" to Res.string.category_transporte,
    "Lazer" to Res.string.category_lazer,
    "Saude" to Res.string.category_saude,
    "Educacao" to Res.string.category_educacao,
    "Moradia" to Res.string.category_moradia,
    "Salario" to Res.string.category_salario,
    "Freelance" to Res.string.category_freelance,
    "Investimentos" to Res.string.category_investimentos,
    "Outros" to Res.string.category_outros,
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
