package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.hhldiniz.praondefoiomeudinheiro.platform.CurrencyFormatter
import com.hhldiniz.praondefoiomeudinheiro.platform.currencyFormatter
import com.hhldiniz.praondefoiomeudinheiro.platform.rememberSpreadsheetFilePicker
import com.hhldiniz.praondefoiomeudinheiro.platform.rememberSpreadsheetFolderPicker
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import kotlinx.coroutines.launch
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.presentation.components.localizedCategoryName
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalRed
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PraOndeFoiOMeuDinheiroTheme
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoTag
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.action_apply
import com.hhldiniz.praondefoiomeudinheiro.resources.action_clear
import com.hhldiniz.praondefoiomeudinheiro.resources.bottom_nav_entries
import com.hhldiniz.praondefoiomeudinheiro.resources.bottom_nav_summary
import com.hhldiniz.praondefoiomeudinheiro.resources.chart_tab_earnings
import com.hhldiniz.praondefoiomeudinheiro.resources.chart_tab_spending
import com.hhldiniz.praondefoiomeudinheiro.resources.entry_type_expense
import com.hhldiniz.praondefoiomeudinheiro.resources.entry_type_income
import com.hhldiniz.praondefoiomeudinheiro.resources.filter_category_all
import com.hhldiniz.praondefoiomeudinheiro.resources.filter_category_selected
import com.hhldiniz.praondefoiomeudinheiro.resources.filter_dialog_title
import com.hhldiniz.praondefoiomeudinheiro.resources.filter_label_category
import com.hhldiniz.praondefoiomeudinheiro.resources.filter_label_period
import com.hhldiniz.praondefoiomeudinheiro.resources.home_add_entry_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.home_date_cancel
import com.hhldiniz.praondefoiomeudinheiro.resources.home_date_from
import com.hhldiniz.praondefoiomeudinheiro.resources.home_date_ok
import com.hhldiniz.praondefoiomeudinheiro.resources.home_date_placeholder
import com.hhldiniz.praondefoiomeudinheiro.resources.home_date_to
import com.hhldiniz.praondefoiomeudinheiro.resources.home_entries_empty
import com.hhldiniz.praondefoiomeudinheiro.resources.home_filter_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.home_menu_add_manually
import com.hhldiniz.praondefoiomeudinheiro.resources.home_menu_import_file
import com.hhldiniz.praondefoiomeudinheiro.resources.home_menu_import_folder
import com.hhldiniz.praondefoiomeudinheiro.resources.home_patrimony
import com.hhldiniz.praondefoiomeudinheiro.resources.home_period_label
import com.hhldiniz.praondefoiomeudinheiro.resources.home_remaining_balance
import com.hhldiniz.praondefoiomeudinheiro.resources.home_scroll_to_top_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.home_title
import com.hhldiniz.praondefoiomeudinheiro.resources.home_total_earnings
import com.hhldiniz.praondefoiomeudinheiro.resources.home_total_spending
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_title

/** How close to the end of the loaded entries the list gets before requesting the next page. */
private const val ENTRIES_PREFETCH_DISTANCE = 5

/**
 * Entry-point composable for the Home screen. Sets up file/folder picker
 * launchers, collects the ViewModel state, and delegates rendering to
 * [HomeContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAddEntry: () -> Unit = {},
    refreshKey: Int = 0,
) {
    val filePicker = rememberSpreadsheetFilePicker { file -> viewModel.importFile(file) }
    val folderPicker = rememberSpreadsheetFolderPicker { folder -> viewModel.importFolder(folder) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            viewModel.refreshData()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val hasMoreEntries by viewModel.hasMoreEntries.collectAsStateWithLifecycle()

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
        onNavigateToAddEntry = onNavigateToAddEntry,
        entries = entries,
        hasMoreEntries = hasMoreEntries,
        onLoadMoreEntries = viewModel::loadMoreEntries,
        allCategories = uiState.allCategories,
        selectedCategory = uiState.selectedCategory,
        onCategorySelected = viewModel::onCategorySelected,
        datasetMinDate = uiState.datasetMinDate,
        datasetMaxDate = uiState.datasetMaxDate,
        onImportFile = { filePicker.launch() },
        onImportFolder = { folderPicker.launch() },
        isImporting = uiState.isImporting,
        importingFileName = uiState.importingFileName,
        importedFiles = uiState.importedFiles,
        importingTotal = uiState.importingTotal,
        modifier = modifier
    )
}

/** Main content layout for the Home screen with summary, charts, entries and import FAB. */
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
    onNavigateToAddEntry: () -> Unit = {},
    entries: List<EntryDisplay>,
    hasMoreEntries: Boolean,
    onLoadMoreEntries: () -> Unit,
    allCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    datasetMinDate: Long?,
    datasetMaxDate: Long?,
    onImportFile: () -> Unit = {},
    onImportFolder: () -> Unit = {},
    isImporting: Boolean = false,
    importingFileName: String? = null,
    importedFiles: List<String> = emptyList(),
    importingTotal: Int = 0,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember(selectedCurrency) { currencyFormatter(selectedCurrency) }
    val remaining = patrimony + totalEarnings - totalSpending
    var showEarnings by remember { mutableStateOf(false) }
    var showEntries by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (!showEntries) {
                Box {
                    FloatingActionButton(
                        onClick = { showImportMenu = true },
                        containerColor = BrutalPink,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(Res.string.home_add_entry_content_description)
                        )
                    }
                    DropdownMenu(
                        expanded = showImportMenu,
                        onDismissRequest = { showImportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.home_menu_add_manually)) },
                            onClick = {
                                showImportMenu = false
                                onNavigateToAddEntry()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.home_menu_import_file)) },
                            onClick = {
                                showImportMenu = false
                                onImportFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.home_menu_import_folder)) },
                            onClick = {
                                showImportMenu = false
                                onImportFolder()
                            }
                        )
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.home_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(Res.string.home_filter_content_description)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(Res.string.settings_title)
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
                    label = { Text(stringResource(Res.string.bottom_nav_summary)) }
                )
                NavigationBarItem(
                    selected = showEntries,
                    onClick = { showEntries = true },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(Res.string.bottom_nav_entries)) }
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
                    entries = entries,
                    hasMore = hasMoreEntries,
                    onLoadMore = onLoadMoreEntries,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PatrimonyCard(
                        patrimony = patrimony,
                        onPatrimonyChanged = onPatrimonyChanged,
                        currencyFormat = currencyFormat,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    RemainingCard(
                        remaining = remaining,
                        currencyFormat = currencyFormat,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                        text = stringResource(Res.string.chart_tab_spending),
                        isActive = !showEarnings,
                        backgroundColor = BrutalPink,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { showEarnings = false },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = stringResource(Res.string.chart_tab_earnings),
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
                                text = stringResource(Res.string.home_total_spending),
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
                                text = stringResource(Res.string.home_total_earnings),
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

    if (isImporting) {
        ImportProgressOverlay(
            importingFileName = importingFileName,
            importedFiles = importedFiles,
            importingTotal = importingTotal,
        )
    }
}

/** A neo-brutalist toggle button used for switching between spending and earnings views. */
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

/** Incrementally paged list of entries with a scroll-to-top button. */
@Composable
private fun EntriesList(
    entries: List<EntryDisplay>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    currencyFormat: CurrencyFormatter,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // Ask the ViewModel for the next page a few rows before the end, which is
    // what androidx.paging's prefetch distance did on Android.
    val shouldLoadMore by remember(entries.size, hasMore) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && lastVisible >= entries.size - ENTRIES_PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    if (entries.isEmpty()) {
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
                    text = stringResource(Res.string.home_entries_empty),
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
            items(
                items = entries,
                key = { it.id },
            ) { entry ->
                EntryCard(
                    entry = entry,
                    currencyFormat = currencyFormat,
                )
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
                        contentDescription = stringResource(Res.string.home_scroll_to_top_content_description),
                        tint = BrutalBlack
                    )
                }
            }
        }
    }
}

/** A single entry card showing date, description, category tag and formatted amount. */
@Composable
private fun EntryCard(
    entry: EntryDisplay,
    currencyFormat: CurrencyFormatter,
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
                        text = formatDayMonthYear(entry.dateMillis),
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
                        val tagText = if (entry.isExpense) stringResource(Res.string.entry_type_expense)
                                      else stringResource(Res.string.entry_type_income)
                        NeoTag(
                            text = tagText,
                            backgroundColor = tagBg,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = localizedCategoryName(entry.category),
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
                    color = if (entry.isExpense) BrutalRed else BrutalCyan,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/** Card displaying (and allowing inline editing of) the user's net worth / patrimony. */
@Composable
private fun PatrimonyCard(
    patrimony: Double,
    onPatrimonyChanged: (Double) -> Unit,
    currencyFormat: CurrencyFormatter,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(patrimony) { mutableStateOf(patrimony.toLong().toString()) }

    HardShadowBox(
        offsetX = 4.dp,
        offsetY = 4.dp,
        fillHeight = true,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .background(BrutalYellow, RectangleShape)
                .padding(12.dp)
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = stringResource(Res.string.home_patrimony),
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

/** Card showing the remaining balance (patrimony + earnings - spending), colour-coded green/red. */
@Composable
private fun RemainingCard(
    remaining: Double,
    currencyFormat: CurrencyFormatter,
    modifier: Modifier = Modifier
) {
    val bgColor = if (remaining >= 0) BrutalCyan else BrutalRed
    val textColor = Color.Black

    HardShadowBox(
        offsetX = 4.dp,
        offsetY = 4.dp,
        fillHeight = true,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .background(bgColor, RectangleShape)
                .padding(12.dp)
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = stringResource(Res.string.home_remaining_balance),
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

/** Dropdown for selecting the time period (Day, Week, Month, Year, Custom). */
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
                text = stringResource(Res.string.home_period_label, stringResource(selectedPeriod.labelRes)),
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

/** Dropdown for filtering entries by category. */
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
                    .background(BrutalCyan, RectangleShape)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedCategory != null)
                        stringResource(Res.string.filter_category_selected, localizedCategoryName(selectedCategory))
                    else stringResource(Res.string.filter_category_all),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
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
                        text = stringResource(Res.string.filter_category_all),
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
                            text = localizedCategoryName(category),
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

/** Alert dialog for applying category and custom date-range filters. */
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
                text = stringResource(Res.string.filter_dialog_title),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.filter_label_period),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DateBox(
                        label = stringResource(Res.string.home_date_from),
                        dateText = tempStartDate?.let { formatDayMonthYear(it) }
                            ?: stringResource(Res.string.home_date_placeholder),
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateBox(
                        label = stringResource(Res.string.home_date_to),
                        dateText = tempEndDate?.let { formatDayMonthYear(it) }
                            ?: stringResource(Res.string.home_date_placeholder),
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = stringResource(Res.string.filter_label_category),
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
                                .background(BrutalCyan, RectangleShape)
                                .clickable { categoryExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tempCategory?.let { localizedCategoryName(it) }
                                    ?: stringResource(Res.string.filter_category_all),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
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
                                    text = stringResource(Res.string.filter_category_all),
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
                                        text = localizedCategoryName(category),
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
                Text(stringResource(Res.string.action_apply), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                tempCategory = null
                tempStartDate = null
                tempEndDate = null
                onApplyCategory(null)
            }) {
                Text(stringResource(Res.string.action_clear), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(Res.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(Res.string.home_date_cancel), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(Res.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(Res.string.home_date_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** Inline date-range selector with start/end date pickers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onRangeSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateBox(
            label = stringResource(Res.string.home_date_from),
            dateText = startDate?.let { formatDayMonthYear(it) } ?: stringResource(Res.string.home_date_placeholder),
            onClick = { showStartPicker = true },
            modifier = Modifier.weight(1f)
        )
        DateBox(
            label = stringResource(Res.string.home_date_to),
            dateText = endDate?.let { formatDayMonthYear(it) } ?: stringResource(Res.string.home_date_placeholder),
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
                    Text(stringResource(Res.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(Res.string.home_date_cancel), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(Res.string.home_date_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(Res.string.home_date_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** A clickable box displaying a date label and value, used in date-range selectors. */
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


