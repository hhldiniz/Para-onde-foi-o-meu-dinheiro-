package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.PatrimonyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.SelectedFilesHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.TransactionValueParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.platform.currentRegionCode
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import com.hhldiniz.praondefoiomeudinheiro.util.isoWeekOfYear
import com.hhldiniz.praondefoiomeudinheiro.util.localDateOf
import com.hhldiniz.praondefoiomeudinheiro.util.minusMonths
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher as platformIoDispatcher

private data class FilterParams(
    val startMillis: Long,
    val endMillis: Long,
    val selectedCategory: String?,
)

/** One side (spending or earnings) of the charts for the active filter. */
private data class Series(
    val points: List<SpendingDataPoint>,
    val categories: List<CategorySpending>,
    val total: Double,
)

// Both parsers live in TransactionValueParser, shared with the automatic
// importer, so the two import paths agree on what counts as a date and an
// amount (named months, `1.234,56`, parentheses for negatives, ...).
private fun CsvEntry.toImportedEntry(isExpense: Boolean, fileName: String): ImportedEntry? {
    val dateMillis = TransactionValueParser.parseDate(date) ?: return null
    val parsedAmount = TransactionValueParser.parseAmount(amount) ?: return null
    return ImportedEntry(
        dateMillis = dateMillis,
        amount = parsedAmount,
        description = description,
        category = category,
        isExpense = isExpense,
        fileName = fileName,
    )
}

private fun ImportedEntry.toDisplay() = EntryDisplay(
    dateMillis = dateMillis,
    description = description,
    category = category,
    amount = amount,
    isExpense = isExpense,
    id = id,
)

internal fun deriveCategoriesToInsert(
    entries: List<ImportedEntry>,
    existing: Set<String>,
): List<String> {
    return entries
        .map { it.category }
        .filter { it.isNotBlank() }
        .distinct()
        .filter { !existing.contains(it) }
}

class HomeViewModel(
    private val importRepository: ImportRepository,
    private val repository: SpreadsheetRepository,
    private val categoryRepository: CategoryRepository,
    private val ioDispatcher: CoroutineDispatcher = platformIoDispatcher,
) : ViewModel() {

    private suspend fun saveCategoriesFromEntries(entries: List<ImportedEntry>): Int {
        val existing = categoryRepository.getAllSync().map { it.name }.toSet()
        val toInsert = deriveCategoriesToInsert(entries, existing)
        if (toInsert.isNotEmpty()) categoryRepository.insertAll(toInsert)
        return toInsert.size
    }

    private val _uiState = MutableStateFlow(HomeUiState(selectedCurrency = CurrencyHolder.selectedCurrency.value))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var filterParams = FilterParams(startMillis = 0L, endMillis = 0L, selectedCategory = null)

    // Entries are paged straight out of SQLite with LIMIT/OFFSET rather than
    // pulled into memory whole. androidx.paging is Android-only, so the list
    // keeps its own cursor here and the UI asks for the next page when it
    // scrolls near the end.
    private val _entries = MutableStateFlow<List<EntryDisplay>>(emptyList())
    val entries: StateFlow<List<EntryDisplay>> = _entries.asStateFlow()

    private val _hasMoreEntries = MutableStateFlow(true)
    val hasMoreEntries: StateFlow<Boolean> = _hasMoreEntries.asStateFlow()

    private var loadedEntryCount = 0
    private var pageLoadInFlight = false

    /** Bumped whenever the filter changes, so a page that is still loading is discarded. */
    private var entriesGeneration = 0

    init {
        viewModelScope.launch {
            CurrencyHolder.selectedCurrency.collect { currency ->
                _uiState.update { it.copy(selectedCurrency = currency) }
            }
        }
        viewModelScope.launch {
            DataClearedHolder.cleared.collect { cleared ->
                if (cleared) {
                    showZeroedState()
                }
            }
        }
        viewModelScope.launch {
            PatrimonyHolder.patrimony.collect { value ->
                _uiState.update { it.copy(patrimony = value) }
            }
        }
    }

    /**
     * Imports whatever the landing flow selected, falling back to the data
     * already stored (or a zeroed state) when nothing was selected.
     */
    fun loadData() {
        val selectedFiles = SelectedFilesHolder.files
        // Files picked after a "clear all data" are legitimate: clearing empties
        // SelectedFilesHolder itself (see clearAllAppData), so anything here now
        // was chosen by the landing flow afterwards and must still be imported.
        if (selectedFiles.isEmpty()) {
            refreshData()
            return
        }
        viewModelScope.launch { importFiles(selectedFiles, zeroWhenEmpty = true) }
    }

    /** Imports a single spreadsheet the user picked from the Home screen. */
    fun importFile(file: PlatformFile) {
        viewModelScope.launch { importFiles(listOf(file)) }
    }

    /** Imports every spreadsheet inside a folder the user picked. */
    fun importFolder(folder: PlatformFolder) {
        viewModelScope.launch {
            importFiles(withContext(ioDispatcher) { folder.listSpreadsheetFiles() })
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val hasStoredData = withContext(ioDispatcher) { importRepository.count() > 0 }
            if (hasStoredData && !DataClearedHolder.cleared.value) {
                updateDerivedState()
                loadDataForPeriod(Period.MONTH)
            } else {
                showZeroedState()
            }
        }
    }

    /**
     * Reads, stores and shows [files], reporting progress through the import
     * overlay and a per-file summary through `debugMessage`. With
     * [zeroWhenEmpty], a batch that yields no entry leaves a zeroed screen
     * rather than whatever was shown before.
     */
    private suspend fun importFiles(files: List<PlatformFile>, zeroWhenEmpty: Boolean = false) {
        setImporting(total = files.size)
        val entries = mutableListOf<ImportedEntry>()
        val rawAmounts = mutableListOf<String>()
        val messages = mutableListOf<String>()
        for (file in files) {
            val fileName = file.name
            _uiState.update { it.copy(importingFileName = fileName) }
            withContext(ioDispatcher) { repository.readValues(file) }
                .onSuccess { range ->
                    range.spendingEntries.mapTo(rawAmounts) { it.amount }
                    range.earningsEntries.mapTo(rawAmounts) { it.amount }
                    val spending = range.spendingEntries.mapNotNull { it.toImportedEntry(isExpense = true, fileName = fileName) }
                    val earnings = range.earningsEntries.mapNotNull { it.toImportedEntry(isExpense = false, fileName = fileName) }
                    entries += spending
                    entries += earnings
                    messages += "$fileName: ${spending.size} gastos, ${earnings.size} rendas"
                }
                .onFailure { error -> messages += "Falha ao ler ${file.identifier}: ${error.message}" }
            markImported(fileName)
        }
        if (entries.isNotEmpty()) {
            withContext(ioDispatcher) {
                val inserted = importRepository.insertEntries(entries)
                messages += "Importado: ${inserted.size} registros (${entries.size - inserted.size} duplicatas ignoradas)"
                val newCategories = saveCategoriesFromEntries(entries)
                if (newCategories > 0) {
                    messages += "$newCategories categoria(s) nova(s) salva(s) do arquivo"
                }
            }
        } else {
            messages += "Nenhum dado encontrado nos arquivos."
        }
        detectCurrency(rawAmounts)
        _uiState.update { it.copy(debugMessage = messages.joinToString("\n")) }
        if (entries.isNotEmpty()) {
            // Refreshed even when every entry was a duplicate: the stored data is
            // what the screen shows, and it is exactly what the files contain.
            DataClearedHolder.reset()
            updateDerivedState()
            loadDataForPeriod(_uiState.value.selectedPeriod)
        } else if (zeroWhenEmpty) {
            showZeroedState()
        }
        clearImporting()
    }

    private fun setImporting(total: Int) {
        _uiState.update {
            it.copy(
                isImporting = true,
                importingFileName = null,
                importingTotal = total,
                importedFiles = emptyList(),
            )
        }
    }

    private fun markImported(fileName: String) {
        _uiState.update {
            val updated = (it.importedFiles + fileName).distinct()
            it.copy(importedFiles = updated, importingFileName = null)
        }
    }

    private fun clearImporting() {
        _uiState.update {
            it.copy(
                isImporting = false,
                importingFileName = null,
                importedFiles = emptyList(),
                importingTotal = 0,
            )
        }
    }

    private fun showZeroedState() {
        _uiState.update {
            it.copy(
                spendingData = emptyList(),
                categorySpending = emptyList(),
                earningsData = emptyList(),
                categoryEarnings = emptyList(),
                totalSpending = 0.0,
                totalEarnings = 0.0,
                allCategories = emptyList(),
                datasetMinDate = null,
                datasetMaxDate = null,
                debugMessage = null,
            )
        }
        loadDataForPeriod(Period.MONTH)
    }

    fun onPeriodSelected(period: Period) {
        if (period != _uiState.value.selectedPeriod) {
            if (period == Period.CUSTOM) {
                val endDate = currentTimeMillis()
                filterByDateRange(minusMonths(endDate, 3), endDate)
            } else {
                loadDataForPeriod(period)
            }
        }
    }

    fun onCustomDateRange(startDate: Long, endDate: Long) {
        filterByDateRange(startDate, endDate)
    }

    fun onPatrimonyChanged(value: Double) {
        PatrimonyHolder.setPatrimony(value)
    }

    fun onCurrencyChanged(currency: CurrencyOption) {
        CurrencyHolder.setCurrency(currency)
    }

    fun onCategorySelected(category: String?) {
        val currentState = _uiState.value
        val period = currentState.selectedPeriod
        if (period == Period.CUSTOM) {
            filterAndEmit(
                period = period,
                startMillis = currentState.customStartDate ?: 0L,
                endMillis = currentState.customEndDate ?: 0L,
                customStart = currentState.customStartDate,
                customEnd = currentState.customEndDate,
                selectedCategory = category,
            )
        } else {
            loadDataForPeriod(period, category)
        }
    }

    /** Loads the next page of entries for the active filter; a no-op once the end is reached. */
    fun loadMoreEntries() {
        if (pageLoadInFlight || !_hasMoreEntries.value) return
        pageLoadInFlight = true
        val generation = entriesGeneration
        val params = filterParams
        viewModelScope.launch {
            val page = try {
                withContext(ioDispatcher) {
                    importRepository.getEntriesPage(
                        category = params.selectedCategory,
                        startMillis = params.startMillis,
                        endMillis = params.endMillis,
                        limit = ENTRIES_PAGE_SIZE,
                        offset = loadedEntryCount,
                    )
                }
            } catch (_: Exception) {
                null
            } finally {
                pageLoadInFlight = false
            }

            if (generation != entriesGeneration) {
                // The filter changed while this page was loading: drop it and
                // start the first page of the new filter, which resetEntries()
                // could not kick off while this load was in flight.
                loadMoreEntries()
                return@launch
            }
            if (page == null) {
                // Stop paging rather than retry in a loop; the list keeps what it has.
                _hasMoreEntries.value = false
                return@launch
            }

            loadedEntryCount += page.size
            _entries.update { it + page.map { entry -> entry.toDisplay() } }
            _hasMoreEntries.value = page.size == ENTRIES_PAGE_SIZE
        }
    }

    private fun resetEntries() {
        entriesGeneration++
        loadedEntryCount = 0
        _entries.value = emptyList()
        _hasMoreEntries.value = true
        loadMoreEntries()
    }

    private fun updateDerivedState() {
        viewModelScope.launch {
            val (allCategories, minDate, maxDate) = withContext(ioDispatcher) {
                val categories = importRepository.getDistinctCategories().sorted()
                val minDate = importRepository.getMinDate()
                val maxDate = importRepository.getMaxDate()
                Triple(categories, minDate, maxDate)
            }
            _uiState.update { it.copy(allCategories = allCategories, datasetMinDate = minDate, datasetMaxDate = maxDate) }
        }
    }

    private fun loadDataForPeriod(period: Period, category: String? = _uiState.value.selectedCategory) {
        viewModelScope.launch {
            val (start, end) = withContext(ioDispatcher) {
                val min = importRepository.getMinDate() ?: currentTimeMillis()
                val max = importRepository.getMaxDate() ?: min
                min to max
            }
            filterAndEmit(period, start, end, null, null, category)
        }
    }

    private fun filterByDateRange(startDate: Long, endDate: Long) {
        filterAndEmit(Period.CUSTOM, startDate, endDate, startDate, endDate, _uiState.value.selectedCategory)
    }

    private fun filterAndEmit(
        period: Period,
        startMillis: Long,
        endMillis: Long,
        customStart: Long?,
        customEnd: Long?,
        selectedCategory: String?,
    ) {
        filterParams = FilterParams(startMillis, endMillis, selectedCategory)
        resetEntries()
        viewModelScope.launch {
            val (spending, earnings) = withContext(ioDispatcher) {
                loadSeries(isExpense = true, selectedCategory, startMillis, endMillis, period) to
                    loadSeries(isExpense = false, selectedCategory, startMillis, endMillis, period)
            }

            _uiState.update {
                it.copy(
                    spendingData = spending.points,
                    categorySpending = spending.categories,
                    earningsData = earnings.points,
                    categoryEarnings = earnings.categories,
                    selectedPeriod = period,
                    totalSpending = spending.total,
                    totalEarnings = earnings.total,
                    customStartDate = customStart,
                    customEndDate = customEnd,
                    selectedCategory = selectedCategory,
                )
            }
        }
    }

    private suspend fun loadSeries(
        isExpense: Boolean,
        category: String?,
        startMillis: Long,
        endMillis: Long,
        period: Period,
    ): Series {
        val total = importRepository.getCategoryTotals(isExpense, category, startMillis, endMillis).sumOf { it.total }
        val entries = importRepository.getEntriesByDateRange(isExpense, category, startMillis, endMillis)
        val (points, categories) = buildChartData(entries, period)
        return Series(points, categories, total)
    }

    private fun buildChartData(
        entries: List<ImportedEntry>,
        period: Period,
    ): Pair<List<SpendingDataPoint>, List<CategorySpending>> {
        if (entries.isEmpty()) return emptyList<SpendingDataPoint>() to emptyList()

        val total = entries.sumOf { it.amount }

        val lineData = when (period) {
            Period.DAY -> groupByDay(entries)
            Period.WEEK -> groupByWeek(entries)
            Period.MONTH, Period.CUSTOM -> {
                val spanMillis = entries.maxOf { it.dateMillis } - entries.minOf { it.dateMillis }
                val spanDays = (spanMillis / (1000.0 * 60 * 60 * 24)).toInt()
                when {
                    spanDays > 35 -> entries.groupBy { entry ->
                        MONTH_NAMES[localDateOf(entry.dateMillis).month - 1]
                    }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                    spanDays > 7 -> groupByWeek(entries)
                    else -> groupByDay(entries)
                }
            }
            Period.YEAR -> {
                entries.groupBy { entry -> localDateOf(entry.dateMillis).year.toString() }
                    .map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                    .sortedBy { it.label }
            }
        }

        val categories = if (total > 0) {
            entries.groupBy { it.category }
                .map { (cat, list) -> CategorySpending(cat, list.sumOf { it.amount }) }
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
        } else {
            emptyList()
        }

        return lineData to categories
    }

    private fun groupByDay(entries: List<ImportedEntry>): List<SpendingDataPoint> =
        entries.groupBy { entry ->
            val date = localDateOf(entry.dateMillis)
            "${date.day}/${date.month}"
        }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
            .sortedBy {
                val parts = it.label.split("/")
                (parts.getOrNull(1)?.toIntOrNull() ?: 0) * 100 + (parts.getOrNull(0)?.toIntOrNull() ?: 0)
            }

    private fun groupByWeek(entries: List<ImportedEntry>): List<SpendingDataPoint> =
        entries.groupBy { entry -> "Sem ${isoWeekOfYear(entry.dateMillis)}" }
            .map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
            .sortedBy { it.label.removePrefix("Sem ").toIntOrNull() ?: 0 }

    private fun detectCurrency(rawAmounts: List<String>) {
        val detected = rawAmounts.mapNotNull { CurrencyOption.fromAmountString(it) }
        if (detected.isNotEmpty()) {
            val majority = detected.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
            CurrencyHolder.setCurrency(majority)
            return
        }
        val commaCount = rawAmounts.count { it.contains(",") }
        val dotCount = rawAmounts.count { it.contains(".") && !it.contains(",") }
        if (commaCount > dotCount && commaCount > rawAmounts.size / 2) {
            CurrencyHolder.setCurrency(
                if (currentRegionCode() == "AR") CurrencyOption.ARS else CurrencyOption.BRL
            )
        } else if (dotCount > commaCount && dotCount > rawAmounts.size / 2) {
            CurrencyHolder.setCurrency(CurrencyOption.USD)
        }
    }

    private companion object {
        const val ENTRIES_PAGE_SIZE = 30
        val MONTH_NAMES = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
    }
}
