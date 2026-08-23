package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentClass
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import com.hhldiniz.praondefoiomeudinheiro.platform.CurrencyFormatter
import com.hhldiniz.praondefoiomeudinheiro.platform.currencyFormatter
import com.hhldiniz.praondefoiomeudinheiro.presentation.components.localizedInvestmentClass
import com.hhldiniz.praondefoiomeudinheiro.presentation.components.localizedInvestmentType
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.CategorySpending
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.SpendingPieChart
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalRed
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoButton
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoTag
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.action_cancel
import com.hhldiniz.praondefoiomeudinheiro.resources.action_ok
import com.hhldiniz.praondefoiomeudinheiro.resources.action_save
import com.hhldiniz.praondefoiomeudinheiro.resources.action_saving
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_amount_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_delete_confirm
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_delete_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_delete_text
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_delete_title
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_edit_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_field_current_value
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_field_date
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_field_institution
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_field_invested_amount
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_field_name
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_field_notes
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_field_type
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_form_edit_title
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_form_new_title
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_institution_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_name_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_notes_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_allocation_title
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_btn_add
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_current_value
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_empty
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_list_title
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_manual_hint
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_profit
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_title
import com.hhldiniz.praondefoiomeudinheiro.resources.investments_total_invested

/**
 * The investments tab: the portfolio's totals, how it is split across types,
 * and the positions themselves. Everything is entered by hand — there is no
 * quote provider — so each row is editable and the "current value" is
 * whatever the user last typed.
 *
 * Unlike the other feature screens this one is not a navigation destination:
 * it renders inside Home's third bottom-bar tab, which is why it brings no
 * Scaffold or top bar of its own and adds through an in-content button rather
 * than Home's import FAB.
 */
@Composable
fun InvestmentsScreen(
    modifier: Modifier = Modifier,
    viewModel: InvestmentsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InvestmentsContent(
        state = uiState,
        onAddClicked = viewModel::onAddClicked,
        onEditClicked = viewModel::onEditClicked,
        onDeleteClicked = viewModel::onDeleteClicked,
        modifier = modifier,
    )

    uiState.form?.let { form ->
        InvestmentFormDialog(
            form = form,
            isSaving = uiState.isSaving,
            onNameChanged = viewModel::onNameChanged,
            onTypeChanged = viewModel::onTypeChanged,
            onInstitutionChanged = viewModel::onInstitutionChanged,
            onInvestedAmountChanged = viewModel::onInvestedAmountChanged,
            onCurrentValueChanged = viewModel::onCurrentValueChanged,
            onDateChanged = viewModel::onDateChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onConfirm = viewModel::save,
            onDismiss = viewModel::onFormDismissed,
        )
    }

    if (uiState.pendingDeleteId != null) {
        DeleteInvestmentDialog(
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }
}

@Composable
private fun InvestmentsContent(
    state: InvestmentsUiState,
    onAddClicked: () -> Unit,
    onEditClicked: (Investment) -> Unit,
    onDeleteClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currencyFormat = remember(state.selectedCurrency) { currencyFormatter(state.selectedCurrency) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.investments_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TotalCard(
                label = stringResource(Res.string.investments_total_invested),
                value = currencyFormat.format(state.totalInvested),
                backgroundColor = BrutalYellow,
                textColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            TotalCard(
                label = stringResource(Res.string.investments_current_value),
                value = currencyFormat.format(state.totalCurrent),
                backgroundColor = BrutalCyan,
                textColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ProfitCard(
            profit = state.totalProfit,
            profitPercent = state.totalProfitPercent,
            currencyFormat = currencyFormat,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.allocation.isNotEmpty()) {
            // SpendingPieChart takes already-resolved labels, so the types are
            // localized here rather than inside the chart.
            val slices = state.allocation.map { allocation ->
                CategorySpending(
                    category = localizedInvestmentType(allocation.type),
                    value = allocation.value,
                )
            }
            SpendingPieChart(
                data = slices,
                currencySymbol = state.selectedCurrency.symbol,
                title = stringResource(Res.string.investments_allocation_title),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        NeoButton(
            onClick = onAddClicked,
            backgroundColor = BrutalPink,
            textColor = MaterialTheme.colorScheme.onPrimary,
            text = stringResource(Res.string.investments_btn_add),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.investments.isEmpty()) {
            EmptyPortfolio(modifier = Modifier.fillMaxWidth())
        } else {
            Text(
                text = stringResource(Res.string.investments_list_title, state.investments.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.investments.forEach { investment ->
                InvestmentRow(
                    investment = investment,
                    currencyFormat = currencyFormat,
                    onEdit = { onEditClicked(investment) },
                    onDelete = { onDeleteClicked(investment.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = stringResource(Res.string.investments_manual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TotalCard(
    label: String,
    value: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    HardShadowBox(
        offsetX = 4.dp,
        offsetY = 4.dp,
        fillHeight = true,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .background(backgroundColor, RectangleShape)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.8f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                )
            }
        }
    }
}

/** The portfolio's result, coloured by direction: cyan while up, red once down. */
@Composable
private fun ProfitCard(
    profit: Double,
    profitPercent: Double,
    currencyFormat: CurrencyFormatter,
    modifier: Modifier = Modifier,
) {
    val isLoss = profit < 0.0
    HardShadowBox(
        offsetX = 5.dp,
        offsetY = 5.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isLoss) BrutalRed else BrutalCyan, RectangleShape)
                .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.investments_profit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = signedAmount(profit, currencyFormat),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                text = signedPercent(profitPercent),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun EmptyPortfolio(modifier: Modifier = Modifier) {
    HardShadowBox(
        offsetX = 4.dp,
        offsetY = 4.dp,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightBackground, RectangleShape)
                .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.investments_empty),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = BrutalBlack,
            )
        }
    }
}

@Composable
private fun InvestmentRow(
    investment: Investment,
    currencyFormat: CurrencyFormatter,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoss = investment.profit < 0.0

    HardShadowBox(
        offsetX = 3.dp,
        offsetY = 3.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RectangleShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = investment.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleOf(investment),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(Res.string.investment_edit_content_description),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(Res.string.investment_delete_content_description),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                NeoTag(
                    text = localizedInvestmentType(investment.type),
                    backgroundColor = BrutalYellow,
                    textColor = MaterialTheme.colorScheme.onSecondary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(investment.currentValue),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${signedAmount(investment.profit, currencyFormat)} · ${signedPercent(investment.profitPercent)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isLoss) BrutalRed else BrutalCyan,
                    )
                }
            }
        }
    }
}

private fun subtitleOf(investment: Investment): String {
    val date = formatDayMonthYear(investment.dateMillis)
    return if (investment.institution.isBlank()) date else "${investment.institution} · $date"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentFormDialog(
    form: InvestmentFormState,
    isSaving: Boolean,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (InvestmentType) -> Unit,
    onInstitutionChanged: (String) -> Unit,
    onInvestedAmountChanged: (String) -> Unit,
    onCurrentValueChanged: (String) -> Unit,
    onDateChanged: (Long) -> Unit,
    onNotesChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (form.isEditing) stringResource(Res.string.investment_form_edit_title)
                       else stringResource(Res.string.investment_form_new_title),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // The form is taller than a phone dialog on purpose: capped
                    // and scrollable so the save button stays reachable.
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormTextField(
                    label = stringResource(Res.string.investment_field_name),
                    value = form.name,
                    onValueChanged = onNameChanged,
                    placeholder = stringResource(Res.string.investment_name_placeholder),
                )

                TypeDropdown(
                    selectedType = form.type,
                    onTypeSelected = onTypeChanged,
                )

                FormTextField(
                    label = stringResource(Res.string.investment_field_institution),
                    value = form.institution,
                    onValueChanged = onInstitutionChanged,
                    placeholder = stringResource(Res.string.investment_institution_placeholder),
                )

                FormTextField(
                    label = stringResource(Res.string.investment_field_invested_amount),
                    value = form.investedAmountText,
                    onValueChanged = onInvestedAmountChanged,
                    placeholder = stringResource(Res.string.investment_amount_placeholder),
                    keyboardType = KeyboardType.Decimal,
                )

                FormTextField(
                    label = stringResource(Res.string.investment_field_current_value),
                    value = form.currentValueText,
                    onValueChanged = onCurrentValueChanged,
                    placeholder = stringResource(Res.string.investment_amount_placeholder),
                    keyboardType = KeyboardType.Decimal,
                )

                FieldLabel(text = stringResource(Res.string.investment_field_date))
                HardShadowBox(offsetX = 3.dp, offsetY = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                            .background(LightBackground, RectangleShape)
                            .clickable { showDatePicker = true }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = formatDayMonthYear(form.dateMillis),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrutalBlack,
                        )
                    }
                }

                FormTextField(
                    label = stringResource(Res.string.investment_field_notes),
                    value = form.notes,
                    onValueChanged = onNotesChanged,
                    placeholder = stringResource(Res.string.investment_notes_placeholder),
                )

                form.errorMessageRes?.let { errorRes ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBackground, RectangleShape)
                            .border(2.dp, BrutalRed, RectangleShape)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(errorRes),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = BrutalRed,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                Text(
                    text = if (isSaving) stringResource(Res.string.action_saving)
                           else stringResource(Res.string.action_save),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), fontWeight = FontWeight.Bold)
            }
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = form.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChanged(it) }
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.action_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.action_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DeleteInvestmentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.investment_delete_title),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = { Text(stringResource(Res.string.investment_delete_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.investment_delete_confirm),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = BrutalBlack,
    )
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column {
        FieldLabel(text = label)
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = onValueChanged,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            placeholder = { Text(placeholder) },
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
}

/**
 * The type picker. Types are listed grouped by asset class (fixed income,
 * variable income, other) with a non-clickable heading per group, because a
 * flat list of nineteen products is hard to scan.
 */
@Composable
private fun TypeDropdown(
    selectedType: InvestmentType,
    onTypeSelected: (InvestmentType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        FieldLabel(text = stringResource(Res.string.investment_field_type))
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .background(BrutalCyan, RectangleShape)
                    .clickable { expanded = true }
                    .padding(12.dp)
            ) {
                Text(
                    text = localizedInvestmentType(selectedType),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                InvestmentClass.entries.forEach { assetClass ->
                    Text(
                        text = localizedInvestmentClass(assetClass),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    InvestmentType.entries
                        .filter { it.assetClass == assetClass }
                        .forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = localizedInvestmentType(type),
                                        fontWeight = if (type == selectedType) FontWeight.Black
                                                     else FontWeight.Medium,
                                    )
                                },
                                onClick = {
                                    onTypeSelected(type)
                                    expanded = false
                                }
                            )
                        }
                }
            }
        }
    }
}

/** "+R$ 120,00" / "-R$ 30,00", so a result reads as a direction and not just a number. */
private fun signedAmount(value: Double, currencyFormat: CurrencyFormatter): String {
    val formatted = currencyFormat.format(value)
    return if (value > 0.0) "+$formatted" else formatted
}

/** The same idea for the percentage, rounded to one decimal place. */
private fun signedPercent(percent: Double): String {
    val rounded = (percent * 10).toLong() / 10.0
    val sign = if (rounded > 0.0) "+" else ""
    return "$sign$rounded%"
}
