package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hhldiniz.praondefoiomeudinheiro.presentation.components.localizedCategoryName
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoButton
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PraOndeFoiOMeuDinheiroTheme
import org.koin.compose.viewmodel.koinViewModel
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.action_add
import com.hhldiniz.praondefoiomeudinheiro.resources.intro_categories_add_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.intro_categories_btn_continue
import com.hhldiniz.praondefoiomeudinheiro.resources.intro_categories_instruction
import com.hhldiniz.praondefoiomeudinheiro.resources.intro_categories_title

/**
 * Second onboarding screen: lets the user pick which suggested categories to
 * start with (all selected by default) and add their own.
 */
@Composable
fun IntroCategoriesScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntroCategoriesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.confirmed) {
        onContinue()
        return
    }

    IntroCategoriesContent(
        availableCategories = uiState.availableCategories,
        selectedCategories = uiState.selectedCategories,
        newCategoryText = uiState.newCategoryText,
        onCategoryToggled = viewModel::onCategoryToggled,
        onNewCategoryTextChanged = viewModel::onNewCategoryTextChanged,
        onAddCategory = viewModel::addCustomCategory,
        onContinue = viewModel::onContinue,
        modifier = modifier,
    )
}

@Composable
private fun IntroCategoriesContent(
    availableCategories: List<String>,
    selectedCategories: Set<String>,
    newCategoryText: String,
    onCategoryToggled: (String) -> Unit,
    onNewCategoryTextChanged: (String) -> Unit,
    onAddCategory: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HardShadowBox(
                offsetX = 6.dp,
                offsetY = 6.dp,
            ) {
                Box(
                    modifier = Modifier
                        .background(BrutalYellow, RectangleShape)
                        .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.intro_categories_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.intro_categories_instruction),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(20.dp))

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableCategories.forEach { category ->
                CategoryChip(
                    name = category,
                    selected = category in selectedCategories,
                    onClick = { onCategoryToggled(category) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HardShadowBox(
                offsetX = 3.dp,
                offsetY = 3.dp,
                modifier = Modifier.weight(1f),
            ) {
                TextField(
                    value = newCategoryText,
                    onValueChange = onNewCategoryTextChanged,
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.intro_categories_add_placeholder)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrutalBlack,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LightBackground,
                        unfocusedContainerColor = LightBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrutalBlack,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape),
                )
            }

            HardShadowBox(
                offsetX = 3.dp,
                offsetY = 3.dp,
            ) {
                Box(
                    modifier = Modifier
                        .background(BrutalCyan, RectangleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .clickable(enabled = newCategoryText.isNotBlank(), onClick = onAddCategory)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.action_add),
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        NeoButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            textColor = MaterialTheme.colorScheme.onPrimary,
            text = stringResource(Res.string.intro_categories_btn_continue),
        )
    }
}

/** A toggleable neo-brutalist chip representing a category, highlighted when selected. */
@Composable
private fun CategoryChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    HardShadowBox(
        offsetX = if (selected) 3.dp else 1.dp,
        offsetY = if (selected) 3.dp else 1.dp,
    ) {
        Box(
            modifier = Modifier
                .background(if (selected) BrutalCyan else LightBackground, RectangleShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = localizedCategoryName(name),
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onTertiary else BrutalBlack,
            )
        }
    }
}

@Preview
@Composable
private fun IntroCategoriesContentPreview() {
    val names = listOf(
        "Alimentacao", "Transporte", "Lazer", "Saude", "Educacao",
        "Moradia", "Salario", "Freelance", "Investimentos", "Outros",
    )
    PraOndeFoiOMeuDinheiroTheme {
        IntroCategoriesContent(
            availableCategories = names,
            selectedCategories = names.toSet() - "Freelance",
            newCategoryText = "",
            onCategoryToggled = {},
            onNewCategoryTextChanged = {},
            onAddCategory = {},
            onContinue = {},
        )
    }
}
