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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalOrange
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalRed
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AddEntryViewModel = viewModel(),
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
                        text = "Adicionar Movimentacao",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
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

            if (uiState.errorMessage != null) {
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
                            text = uiState.errorMessage!!,
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
                        .background(BrutalOrange, RectangleShape)
                        .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .clickable(enabled = !uiState.isSaving) { viewModel.save() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isSaving) "Salvando..." else "SALVAR",
                        color = Color.White,
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
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showPicker by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Data",
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
                    text = dateFormat.format(Date(dateMillis)),
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
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar", fontWeight = FontWeight.Bold)
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
            text = "Valor",
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
                placeholder = { Text("0,00") },
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
            text = "Descricao",
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
                placeholder = { Text("Descricao da movimentacao") },
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
            text = "Categoria",
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
                            if (selectedCategory.isNotBlank()) BrutalOrange else LightBackground,
                            RectangleShape,
                        )
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (selectedCategory.isNotBlank()) selectedCategory
                               else "Selecione uma categoria",
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
                                    text = category,
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
                                text = "Adicionar nova categoria...",
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
                text = "Nova categoria",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            TextField(
                value = newCategoryName,
                onValueChange = onNameChanged,
                singleLine = true,
                placeholder = { Text("Nome da categoria") },
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
                Text("Adicionar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontWeight = FontWeight.Bold)
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
            text = "Tipo",
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
                        text = "GASTO",
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
                        text = "RENDA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!isExpense) FontWeight.Black else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
    }
}
