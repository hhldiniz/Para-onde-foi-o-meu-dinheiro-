package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.components.localizedCategoryName
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalRed
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.action_add
import com.hhldiniz.praondefoiomeudinheiro.resources.action_back
import com.hhldiniz.praondefoiomeudinheiro.resources.action_cancel
import com.hhldiniz.praondefoiomeudinheiro.resources.action_ok
import com.hhldiniz.praondefoiomeudinheiro.resources.action_save
import com.hhldiniz.praondefoiomeudinheiro.resources.action_saving
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_add_new_category
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_amount_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_category_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_description_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_field_amount
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_field_category
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_field_date
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_field_description
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_field_type
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_new_category_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_new_category_title
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_title
import com.hhldiniz.praondefoiomeudinheiro.resources.entry_type_expense
import com.hhldiniz.praondefoiomeudinheiro.resources.entry_type_income

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AddEntryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.savedSuccessfully) {
        onNavigateBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.add_entry_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrutalYellow,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DateField(
                dateMillis = uiState.dateMillis,
                onDateChanged = viewModel::onDateChanged,
            )

            AmountField(
                value = uiState.amountText,
                onValueChanged = viewModel::onAmountChanged,
            )

            DescriptionField(
                value = uiState.description,
                onValueChanged = viewModel::onDescriptionChanged,
            )

            CategoryDropdown(
                selectedCategory = uiState.category,
                categories = uiState.categories,
                onCategorySelected = viewModel::onCategoryChanged,
                onAddNewCategory = { viewModel.onShowAddCategoryDialog(true) },
            )

            TypeToggle(
                isExpense = uiState.isExpense,
                onTypeChanged = viewModel::onTypeChanged,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.showAddCategoryDialog) {
                AddCategoryDialog(
                    newCategoryName = uiState.newCategoryName,
                    onNameChanged = viewModel::onNewCategoryNameChanged,
                    onConfirm = viewModel::addNewCategory,
                    onDismiss = { viewModel.onShowAddCategoryDialog(false) },
                )
            }

            if (uiState.errorMessageRes != null) {
                HardShadowBox(
                    offsetX = 3.dp,
                    offsetY = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBackground, RectangleShape)
                            .border(2.dp, BrutalRed, RectangleShape)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(uiState.errorMessageRes!!),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = BrutalRed,
                        )
                    }
                }
            }

            HardShadowBox(
                offsetX = 4.dp,
                offsetY = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrutalPink, RectangleShape)
                        .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .clickable(enabled = !uiState.isSaving) { viewModel.save() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isSaving) stringResource(Res.string.action_saving)
                               else stringResource(Res.string.action_save),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    dateMillis: Long,
    onDateChanged: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(Res.string.add_entry_field_date),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = BrutalBlack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .background(LightBackground, RectangleShape)
                    .clickable { showPicker = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = formatDayMonthYear(dateMillis),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrutalBlack,
                )
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChanged(it) }
                    showPicker = false
                }) {
                    Text(stringResource(Res.string.action_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(Res.string.action_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    onValueChanged: (String) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.add_entry_field_amount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = BrutalBlack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = value,
                onValueChange = onValueChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.add_entry_amount_placeholder)) },
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
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
            )
        }
    }
}

@Composable
private fun DescriptionField(
    value: String,
    onValueChanged: (String) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.add_entry_field_description),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = BrutalBlack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = value,
                onValueChange = onValueChanged,
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.add_entry_description_placeholder)) },
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
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
            )
        }
    }
}

@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    onAddNewCategory: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(Res.string.add_entry_field_category),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = BrutalBlack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .background(
                            if (selectedCategory.isNotBlank()) BrutalCyan else LightBackground,
                            RectangleShape,
                        )
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (selectedCategory.isNotBlank()) localizedCategoryName(selectedCategory)
                               else stringResource(Res.string.add_entry_category_placeholder),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategory.isNotBlank()) Color.White else BrutalBlack,
                        ),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = localizedCategoryName(category),
                                    fontWeight = if (category == selectedCategory) FontWeight.Black
                                                 else FontWeight.Medium,
                                )
                            },
                            onClick = {
                                onCategorySelected(category)
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(Res.string.add_entry_add_new_category),
                                fontWeight = FontWeight.Bold,
                                color = BrutalPink,
                            )
                        },
                        onClick = {
                            expanded = false
                            onAddNewCategory()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    newCategoryName: String,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.add_entry_new_category_title),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            TextField(
                value = newCategoryName,
                onValueChange = onNameChanged,
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.add_entry_new_category_placeholder)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrutalBlack,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LightBackground,
                    unfocusedContainerColor = LightBackground,
                    focusedIndicatorColor = BrutalBlack,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = BrutalBlack,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = newCategoryName.isNotBlank(),
            ) {
                Text(stringResource(Res.string.action_add), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TypeToggle(
    isExpense: Boolean,
    onTypeChanged: (Boolean) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.add_entry_field_type),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = BrutalBlack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HardShadowBox(
                offsetX = if (isExpense) 4.dp else 2.dp,
                offsetY = if (isExpense) 4.dp else 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrutalPink, RectangleShape)
                        .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .clickable { onTypeChanged(true) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.entry_type_expense),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isExpense) FontWeight.Black else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            HardShadowBox(
                offsetX = if (!isExpense) 4.dp else 2.dp,
                offsetY = if (!isExpense) 4.dp else 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrutalCyan, RectangleShape)
                        .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .clickable { onTypeChanged(false) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.entry_type_income),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!isExpense) FontWeight.Black else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
    }
}
