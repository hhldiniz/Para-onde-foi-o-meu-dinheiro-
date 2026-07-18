package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.hhldiniz.praondefoiomeudinheiro.R
import kotlinx.coroutines.launch
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalLime
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalOrange
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalRed
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PraOndeFoiOMeuDinheiroTheme
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoTag
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFile(uri, context.contentResolver)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFolder(uri, context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData(context.contentResolver)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entriesPagingItems = viewModel.entriesPagingData.collectAsLazyPagingItems()

    HomeContent(
        spendingData = uiState.spendingData,
        categorySpending = uiState.categorySpending,
        earningsData = uiState.earningsData,
        categoryEarnings = uiState.categoryEarnings,
        selectedPeriod = uiState.selectedPeriod,
        totalSpending = uiState.totalSpending,
        totalEarnings = uiState.totalEarnings,
        patrimony = uiState.patrimony,
        selectedCurrency = uiState.selectedCurrency,
        customStartDate = uiState.customStartDate,
        customEndDate = uiState.customEndDate,
        onPeriodSelected = viewModel::onPeriodSelected,
        onCustomDateRange = viewModel::onCustomDateRange,
        onPatrimonyChanged = viewModel::onPatrimonyChanged,
        debugMessage = uiState.debugMessage,
        onNavigateToSettings = onNavigateToSettings,
        entriesPagingItems = entriesPagingItems,
        allCategories = uiState.allCategories,
        selectedCategory = uiState.selectedCategory,
        onCategorySelected = viewModel::onCategorySelected,
        datasetMinDate = uiState.datasetMinDate,
        datasetMaxDate = uiState.datasetMaxDate,
        onImportFile = { filePickerLauncher.launch(arrayOf("text/*", "*/*")) },
        onImportFolder = { folderPickerLauncher.launch(null) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    spendingData: List<SpendingDataPoint>,
    categorySpending: List<CategorySpending>,
    earningsData: List<SpendingDataPoint>,
    categoryEarnings: List<CategorySpending>,
    selectedPeriod: Period,
    totalSpending: Double,
    totalEarnings: Double,
    patrimony: Double,
    selectedCurrency: CurrencyOption,
    customStartDate: Long?,
    customEndDate: Long?,
    onPeriodSelected: (Period) -> Unit,
    onCustomDateRange: (Long, Long) -> Unit,
    onPatrimonyChanged: (Double) -> Unit,
    debugMessage: String? = null,
    onNavigateToSettings: () -> Unit = {},
    entriesPagingItems: LazyPagingItems<EntryDisplay>,
    allCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    datasetMinDate: Long?,
    datasetMaxDate: Long?,
    onImportFile: () -> Unit = {},
    onImportFolder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember(selectedCurrency) {
        NumberFormat.getCurrencyInstance(selectedCurrency.locale)
    }
    val remaining = patrimony + totalEarnings - totalSpending
    var showEarnings by remember { mutableStateOf(false) }
    var showEntries by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showImportMenu = true },
                    containerColor = BrutalPink,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Importar dados"
                    )
                }
                DropdownMenu(
                    expanded = showImportMenu,
                    onDismissRequest = { showImportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Importar arquivo") },
                        onClick = {
                            showImportMenu = false
                            onImportFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Importar pasta") },
                        onClick = {
                            showImportMenu = false
                            onImportFolder()
                        }
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Filtrar"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrutalYellow,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = !showEntries,
                    onClick = { showEntries = false },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.bottom_nav_summary)) }
                )
                NavigationBarItem(
                    selected = showEntries,
                    onClick = { showEntries = true },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.bottom_nav_entries)) }
                )
            }
        }
    ) { innerPadding ->
        if (showEntries) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                EntriesList(
                    pagingItems = entriesPagingItems,
                    currencyFormat = currencyFormat,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PatrimonyCard(
                        patrimony = patrimony,
                        onPatrimonyChanged = onPatrimonyChanged,
                        currencyFormat = currencyFormat,
                        modifier = Modifier.weight(1f)
                    )
                    RemainingCard(
                        remaining = remaining,
                        currencyFormat = currencyFormat,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PeriodDropdown(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedPeriod == Period.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DateRangeSelector(
                        startDate = customStartDate,
                        endDate = customEndDate,
                        onRangeSelected = onCustomDateRange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton(
                        text = stringResource(R.string.chart_tab_spending),
                        isActive = !showEarnings,
                        backgroundColor = BrutalPink,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { showEarnings = false },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = stringResource(R.string.chart_tab_earnings),
                        isActive = showEarnings,
                        backgroundColor = BrutalCyan,
                        textColor = MaterialTheme.colorScheme.onTertiary,
                        onClick = { showEarnings = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LineChartSwitcher(
                    spendingData = spendingData,
                    earningsData = earningsData,
                    currencySymbol = selectedCurrency.symbol,
                    showEarnings = showEarnings,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                PieChartSwitcher(
                    categorySpending = categorySpending,
                    categoryEarnings = categoryEarnings,
                    currencySymbol = selectedCurrency.symbol,
                    showEarnings = showEarnings,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                HardShadowBox(
                    offsetX = 5.dp,
                    offsetY = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrutalPink, RectangleShape)
                            .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.home_total_spending),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(totalSpending),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HardShadowBox(
                    offsetX = 5.dp,
                    offsetY = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrutalCyan, RectangleShape)
                            .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.home_total_earnings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(totalEarnings),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (debugMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HardShadowBox(
                        offsetX = 3.dp,
                        offsetY = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(LightBackground, RectangleShape)
                                .border(2.dp, BrutalRed, RectangleShape)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = debugMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrutalBlack,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            allCategories = allCategories,
            selectedCategory = selectedCategory,
            customStartDate = customStartDate,
            customEndDate = customEndDate,
            datasetMinDate = datasetMinDate,
            datasetMaxDate = datasetMaxDate,
            onDismiss = { showFilterDialog = false },
            onApplyCategory = onCategorySelected,
            onApplyDateRange = onCustomDateRange,
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    isActive: Boolean,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HardShadowBox(
        offsetX = if (isActive) 5.dp else 2.dp,
        offsetY = if (isActive) 5.dp else 2.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, RectangleShape)
                .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                color = textColor,
            )
        }
    }
}

@Composable
private fun EntriesList(
    pagingItems: LazyPagingItems<EntryDisplay>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    if (pagingItems.itemCount == 0) {
        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .background(LightBackground, RectangleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma entrada encontrada",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrutalBlack
                )
            }
        }
        return
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pagingItems.itemCount) { index ->
                pagingItems[index]?.let { entry ->
                    EntryCard(
                        entry = entry,
                        currencyFormat = currencyFormat,
                        dateFormatter = dateFormatter,
                    )
                }
            }
        }

        if (showScrollToTop) {
            HardShadowBox(
                offsetX = 3.dp,
                offsetY = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(BrutalYellow, RectangleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Ir para o topo",
                        tint = BrutalBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryCard(
    entry: EntryDisplay,
    currencyFormat: NumberFormat,
    dateFormatter: DateTimeFormatter,
) {
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
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatter.format(Instant.ofEpochMilli(entry.dateMillis)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = BrutalBlack.copy(alpha = 0.6f)
                    )
                    if (entry.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrutalBlack
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tagBg = if (entry.isExpense) BrutalPink else BrutalCyan
                        val tagText = if (entry.isExpense) "GASTO" else "RENDA"
                        NeoTag(
                            text = tagText,
                            backgroundColor = tagBg,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = entry.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = BrutalBlack.copy(alpha = 0.5f)
                        )
                    }
                }
                Text(
                    text = currencyFormat.format(entry.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (entry.isExpense) BrutalRed else BrutalLime,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun PatrimonyCard(
    patrimony: Double,
    onPatrimonyChanged: (Double) -> Unit,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(patrimony) { mutableStateOf(patrimony.toLong().toString()) }

    HardShadowBox(
        offsetX = 4.dp,
        offsetY = 4.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .background(BrutalOrange, RectangleShape)
                .padding(12.dp)
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_patrimony),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (isEditing) {
                TextField(
                    value = editText,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() }
                        editText = filtered
                        val parsed = filtered.toDoubleOrNull()
                        if (parsed != null) onPatrimonyChanged(parsed)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = currencyFormat.format(patrimony),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editText = patrimony.toLong().toString()
                            isEditing = true
                        }
                )
            }
        }
    }
}
}

@Composable
private fun RemainingCard(
    remaining: Double,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val bgColor = if (remaining >= 0) BrutalLime else BrutalRed
    val textColor = Color.Black

    HardShadowBox(
        offsetX = 4.dp,
        offsetY = 4.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .background(bgColor, RectangleShape)
                .padding(12.dp)
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_remaining_balance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currencyFormat.format(remaining),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}

@Composable
private fun PeriodDropdown(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        HardShadowBox(
            offsetX = 4.dp,
            offsetY = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .background(BrutalCyan, RectangleShape)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
            Text(
                text = stringResource(R.string.home_period_label, stringResource(selectedPeriod.labelRes)),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiary
            )
        }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Period.entries.forEach { period ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(period.labelRes),
                            fontWeight = if (period == selectedPeriod) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    allCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .background(BrutalOrange, RectangleShape)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedCategory != null) "Categoria: $selectedCategory"
                           else "Todas as categorias",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Todas as categorias",
                        fontWeight = if (selectedCategory == null) FontWeight.Black else FontWeight.Medium
                    )
                },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            allCategories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category,
                            fontWeight = if (category == selectedCategory) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    allCategories: List<String>,
    selectedCategory: String?,
    customStartDate: Long?,
    customEndDate: Long?,
    datasetMinDate: Long?,
    datasetMaxDate: Long?,
    onDismiss: () -> Unit,
    onApplyCategory: (String?) -> Unit,
    onApplyDateRange: (Long, Long) -> Unit,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
    }

    var tempStartDate by remember { mutableStateOf(customStartDate ?: datasetMinDate) }
    var tempEndDate by remember { mutableStateOf(customEndDate ?: datasetMaxDate) }
    var tempCategory by remember { mutableStateOf(selectedCategory) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filtrar",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Período",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DateBox(
                        label = stringResource(R.string.home_date_from),
                        dateText = tempStartDate?.let { dateFormatter.format(Instant.ofEpochMilli(it)) }
                            ?: stringResource(R.string.home_date_placeholder),
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateBox(
                        label = stringResource(R.string.home_date_to),
                        dateText = tempEndDate?.let { dateFormatter.format(Instant.ofEpochMilli(it)) }
                            ?: stringResource(R.string.home_date_placeholder),
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Box {
                    HardShadowBox(
                        offsetX = 2.dp,
                        offsetY = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                                .background(BrutalOrange, RectangleShape)
                                .clickable { categoryExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tempCategory != null) tempCategory!!
                                       else "Todas as categorias",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Todas as categorias",
                                    fontWeight = if (tempCategory == null) FontWeight.Black else FontWeight.Medium
                                )
                            },
                            onClick = {
                                tempCategory = null
                                categoryExpanded = false
                            }
                        )
                        allCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = category,
                                        fontWeight = if (category == tempCategory) FontWeight.Black else FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    tempCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApplyCategory(tempCategory)
                if (tempStartDate != null && tempEndDate != null) {
                    onApplyDateRange(tempStartDate!!, tempEndDate!!)
                }
                onDismiss()
            }) {
                Text("Aplicar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                tempCategory = null
                tempStartDate = null
                tempEndDate = null
                onApplyCategory(null)
            }) {
                Text("Limpar", fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tempStartDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { tempStartDate = it }
                    showStartPicker = false
                }) {
                    Text(stringResource(R.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(R.string.home_date_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tempEndDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { tempEndDate = it }
                    showEndPicker = false
                }) {
                    Text(stringResource(R.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(R.string.home_date_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onRangeSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateBox(
            label = stringResource(R.string.home_date_from),
            dateText = startDate?.let { dateFormatter.format(Instant.ofEpochMilli(it)) } ?: stringResource(R.string.home_date_placeholder),
            onClick = { showStartPicker = true },
            modifier = Modifier.weight(1f)
        )
        DateBox(
            label = stringResource(R.string.home_date_to),
            dateText = endDate?.let { dateFormatter.format(Instant.ofEpochMilli(it)) } ?: stringResource(R.string.home_date_placeholder),
            onClick = { showEndPicker = true },
            modifier = Modifier.weight(1f)
        )
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { sel ->
                        val end = endDate ?: sel
                        if (sel <= end) onRangeSelected(sel, end)
                    }
                    showStartPicker = false
                }) {
                    Text(stringResource(R.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(R.string.home_date_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { sel ->
                        val start = startDate ?: sel
                        if (sel >= start) onRangeSelected(start, sel)
                    }
                    showEndPicker = false
                }) {
                    Text(stringResource(R.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(R.string.home_date_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateBox(
    label: String,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HardShadowBox(
        offsetX = 3.dp,
        offsetY = 3.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .background(MaterialTheme.colorScheme.surface, RectangleShape)
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
        Text(
            text = "$label: $dateText",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
}


