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

private data class ParsedEntry(
    val dateMillis: Long,
    val amount: Double,
    val description: String,
    val category: String,
)

private data class FilterParams(
    val period: Period,
    val startMillis: Long,
    val endMillis: Long,
    val customStart: Long?,
    val customEnd: Long?,
    val selectedCategory: String?,
)

private data class LoadResult(
    val spending: List<ParsedEntry>,
    val earnings: List<ParsedEntry>,
    val errors: List<String>,
    val rawAmounts: List<String>,
)

private fun ImportedEntry.toParsedEntry() = ParsedEntry(dateMillis, amount, description, category)

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

    private val _filterParams = MutableStateFlow(
        FilterParams(
            period = Period.MONTH, startMillis = 0L, endMillis = 0L,
            customStart = null, customEnd = null, selectedCategory = null,
        )
    )

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
     * already stored in Room (or a zeroed state) when nothing was selected.
     */
    fun loadData() {
        val selectedFiles = SelectedFilesHolder.files
        if (selectedFiles.isEmpty() || DataClearedHolder.cleared.value) {
            viewModelScope.launch {
                val hasRoomData = withContext(ioDispatcher) { importRepository.count() > 0 }
                if (hasRoomData && !DataClearedHolder.cleared.value) {
                    loadFromRoom()
                } else {
                    showZeroedState()
                }
            }
            return
        }
        viewModelScope.launch {
            setImporting(fileName = null, total = selectedFiles.size)
            val s = mutableListOf<ParsedEntry>()
            val e = mutableListOf<ParsedEntry>()
            val errs = mutableListOf<String>()
            val amounts = mutableListOf<String>()
            val allImported = mutableListOf<ImportedEntry>()
            for (file in selectedFiles) {
                val fileName = file.name
                _uiState.update { it.copy(importingFileName = fileName) }
                withContext(ioDispatcher) { repository.readValues(file) }
                    .onSuccess { range ->
                        range.spendingEntries.forEach { amounts.add(it.amount) }
                        range.earningsEntries.forEach { amounts.add(it.amount) }
                        val sp = range.spendingEntries.mapNotNull { parseEntry(it) }
                        val ep = range.earningsEntries.mapNotNull { parseEntry(it) }
                        s.addAll(sp)
                        e.addAll(ep)
                        sp.forEach { parsed ->
                            allImported.add(ImportedEntry(dateMillis = parsed.dateMillis, amount = parsed.amount, description = parsed.description, category = parsed.category, isExpense = true, fileName = fileName))
                        }
                        ep.forEach { parsed ->
                            allImported.add(ImportedEntry(dateMillis = parsed.dateMillis, amount = parsed.amount, description = parsed.description, category = parsed.category, isExpense = false, fileName = fileName))
                        }
                        errs.add("CSV OK: ${sp.size} gastos, ${ep.size} rendas de ${range.spendingEntries.size + range.earningsEntries.size} linhas brutas")
                    }
                    .onFailure { error ->
                        errs.add("Falha ao ler ${file.identifier}: ${error.message}")
                    }
                markImported(fileName)
            }
            val result = withContext(ioDispatcher) {
                importRepository.insertEntries(allImported)
                val newCategories = saveCategoriesFromEntries(allImported)
                if (newCategories > 0) {
                    errs.add("$newCategories categoria(s) nova(s) salva(s) do arquivo")
                }
                LoadResult(s, e, errs, amounts)
            }
            _uiState.update { it.copy(debugMessage = result.errors.joinToString("\n")) }
            if (result.spending.isEmpty() && result.earnings.isEmpty()) {
                _uiState.update {
                    it.copy(debugMessage = (it.debugMessage ?: "") + "\nNenhum dado encontrado nos arquivos.")
                }
                showZeroedState()
            } else {
                DataClearedHolder.reset()
                detectCurrency(result.rawAmounts)
                updateDerivedState()
                loadDataForPeriod(Period.MONTH)
            }
            clearImporting()
        }
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            updateDerivedState()
            loadDataForPeriod(Period.MONTH)
        }
    }

    /** Imports a single spreadsheet the user picked from the Home screen. */
    fun importFile(file: PlatformFile) {
        viewModelScope.launch {
            val fileName = file.name
            setImporting(fileName = fileName, total = 1)
            val (imported, errors, rawAmounts) = withContext(ioDispatcher) {
                val errorMessages = mutableListOf<String>()
                val raw = mutableListOf<String>()
                val result = repository.readValues(file)
                if (result.isSuccess) {
                    val range = result.getOrThrow()
                    range.spendingEntries.forEach { raw.add(it.amount) }
                    range.earningsEntries.forEach { raw.add(it.amount) }
                    val sp = range.spendingEntries.mapNotNull { parseEntry(it) }
                    val ep = range.earningsEntries.mapNotNull { parseEntry(it) }
                    val entries = mutableListOf<ImportedEntry>().apply {
                        addAll(sp.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = true, fileName = fileName) })
                        addAll(ep.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = false, fileName = fileName) })
                    }
                    val inserted = importRepository.insertEntries(entries)
                    val newCategories = saveCategoriesFromEntries(entries)
                    errorMessages.add("Importado: ${inserted.size} registros (${entries.size - inserted.size} duplicatas ignoradas)")
                    if (newCategories > 0) {
                        errorMessages.add("$newCategories categoria(s) nova(s) salva(s) do arquivo")
                    }
                    Triple(inserted, errorMessages, raw)
                } else {
                    val error = result.exceptionOrNull()
                    errorMessages.add("Falha ao importar: ${error?.message}")
                    Triple(emptyList<ImportedEntry>(), errorMessages, raw)
                }
            }
            _uiState.update { it.copy(debugMessage = errors.joinToString("\n")) }
            if (imported.isNotEmpty()) {
                DataClearedHolder.reset()
                detectCurrency(rawAmounts)
                updateDerivedState()
                loadDataForPeriod(_uiState.value.selectedPeriod)
            }
            markImported(fileName)
            clearImporting()
        }
    }

    /** Imports every spreadsheet inside a folder the user picked. */
    fun importFolder(folder: PlatformFolder) {
        viewModelScope.launch {
            val files = withContext(ioDispatcher) { folder.listSpreadsheetFiles() }
            setImporting(fileName = null, total = files.size)
            val allEntries = mutableListOf<ImportedEntry>()
            val errorMessages = mutableListOf<String>()
            val raw = mutableListOf<String>()
            for (file in files) {
                val fileName = file.name
                _uiState.update { it.copy(importingFileName = fileName) }
                val result = withContext(ioDispatcher) { repository.readValues(file) }
                if (result.isSuccess) {
                    val range = result.getOrThrow()
                    range.spendingEntries.forEach { raw.add(it.amount) }
                    range.earningsEntries.forEach { raw.add(it.amount) }
                    val sp = range.spendingEntries.mapNotNull { parseEntry(it) }
                    val ep = range.earningsEntries.mapNotNull { parseEntry(it) }
                    sp.forEach { parsed ->
                        allEntries.add(ImportedEntry(dateMillis = parsed.dateMillis, amount = parsed.amount, description = parsed.description, category = parsed.category, isExpense = true, fileName = fileName))
                    }
                    ep.forEach { parsed ->
                        allEntries.add(ImportedEntry(dateMillis = parsed.dateMillis, amount = parsed.amount, description = parsed.description, category = parsed.category, isExpense = false, fileName = fileName))
                    }
                    errorMessages.add("$fileName: ${sp.size} gastos, ${ep.size} rendas")
                } else {
                    val error = result.exceptionOrNull()
                    errorMessages.add("Falha ao ler ${file.identifier}: ${error?.message}")
                }
                markImported(fileName)
            }
            val imported = withContext(ioDispatcher) {
                val inserted = importRepository.insertEntries(allEntries)
                val newCategories = saveCategoriesFromEntries(allEntries)
                errorMessages.add("Total: ${inserted.size} registros (${allEntries.size - inserted.size} duplicatas ignoradas)")
                if (newCategories > 0) {
                    errorMessages.add("$newCategories categoria(s) nova(s) salva(s) do arquivo")
                }
                inserted
            }
            detectCurrency(raw)
            _uiState.update { it.copy(debugMessage = errorMessages.joinToString("\n")) }
            if (imported.isNotEmpty()) {
                DataClearedHolder.reset()
                updateDerivedState()
                loadDataForPeriod(_uiState.value.selectedPeriod)
            }
            clearImporting()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val hasRoomData = withContext(ioDispatcher) { importRepository.count() > 0 }
            if (hasRoomData && !DataClearedHolder.cleared.value) {
                loadFromRoom()
            } else {
                showZeroedState()
            }
        }
    }

    private fun setImporting(fileName: String?, total: Int) {
        _uiState.update {
            it.copy(
                isImporting = true,
                importingFileName = fileName,
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
                kPeriod = period,
                startMillis = currentState.customStartDate ?: 0L,
                endMillis = currentState.customEndDate ?: 0L,
                customStart = currentState.customStartDate,
                customEnd = currentState.customEndDate,
                selectedCategory = category,
            )
        } else {
            viewModelScope.launch {
                val (start, end) = withContext(ioDispatcher) {
                    val min = importRepository.getMinDate() ?: currentTimeMillis()
                    val max = importRepository.getMaxDate() ?: min
                    min to max
                }
                filterAndEmit(period, start, end, null, null, category)
            }
        }
    }

    /** Loads the next page of entries for the active filter; a no-op once the end is reached. */
    fun loadMoreEntries() {
        if (pageLoadInFlight || !_hasMoreEntries.value) return
        pageLoadInFlight = true
        val generation = entriesGeneration
        val params = _filterParams.value
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

    private fun loadDataForPeriod(period: Period) {
        viewModelScope.launch {
            val (start, end) = withContext(ioDispatcher) {
                val min = importRepository.getMinDate() ?: currentTimeMillis()
                val max = importRepository.getMaxDate() ?: min
                min to max
            }
            filterAndEmit(period, start, end, null, null, _uiState.value.selectedCategory)
        }
    }

    private fun filterByDateRange(startDate: Long, endDate: Long) {
        filterAndEmit(Period.CUSTOM, startDate, endDate, startDate, endDate, _uiState.value.selectedCategory)
    }

    private fun filterAndEmit(
        kPeriod: Period,
        startMillis: Long,
        endMillis: Long,
        customStart: Long?,
        customEnd: Long?,
        selectedCategory: String?,
    ) {
        _filterParams.value = FilterParams(kPeriod, startMillis, endMillis, customStart, customEnd, selectedCategory)
        resetEntries()
        viewModelScope.launch {
            val (spendingData, spendingCategories, earningsData, earningsCategories, totalSpending, totalEarnings) =
                withContext(ioDispatcher) {
                    val monthNames = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

                    val spendingCatTotals = importRepository.getCategoryTotals(
                        isExpense = true, category = selectedCategory,
                        startMillis = startMillis, endMillis = endMillis,
                    )
                    val earningsCatTotals = importRepository.getCategoryTotals(
                        isExpense = false, category = selectedCategory,
                        startMillis = startMillis, endMillis = endMillis,
                    )
                    val spendingEntries = importRepository.getEntriesByDateRange(
                        isExpense = true, category = selectedCategory,
                        startMillis = startMillis, endMillis = endMillis,
                    )
                    val earningsEntries = importRepository.getEntriesByDateRange(
                        isExpense = false, category = selectedCategory,
                        startMillis = startMillis, endMillis = endMillis,
                    )
                    val ts = spendingCatTotals.sumOf { it.total }
                    val te = earningsCatTotals.sumOf { it.total }
                    val sd = buildChartData(spendingEntries.map { it.toParsedEntry() }, kPeriod, monthNames)
                    val ed = buildChartData(earningsEntries.map { it.toParsedEntry() }, kPeriod, monthNames)
                    QuadResult(sd.first, sd.second, ed.first, ed.second, ts, te)
                }

            _uiState.update {
                it.copy(
                    spendingData = spendingData,
                    categorySpending = spendingCategories,
                    earningsData = earningsData,
                    categoryEarnings = earningsCategories,
                    selectedPeriod = kPeriod,
                    totalSpending = totalSpending,
                    totalEarnings = totalEarnings,
                    customStartDate = customStart,
                    customEndDate = customEnd,
                    selectedCategory = selectedCategory,
                )
            }
        }
    }

    private data class QuadResult(
        val spendingData: List<SpendingDataPoint>,
        val spendingCategories: List<CategorySpending>,
        val earningsData: List<SpendingDataPoint>,
        val earningsCategories: List<CategorySpending>,
        val totalSpending: Double,
        val totalEarnings: Double,
    )

    private fun buildChartData(
        entries: List<ParsedEntry>,
        period: Period,
        monthNames: List<String>,
    ): Pair<List<SpendingDataPoint>, List<CategorySpending>> {
        if (entries.isEmpty()) return emptyList<SpendingDataPoint>() to emptyList()

        val total = entries.sumOf { it.amount }

        val lineData = when (period) {
            Period.DAY -> groupByDay(entries)
            Period.WEEK -> groupByWeek(entries)
            Period.MONTH, Period.CUSTOM -> {
                val minDate = entries.minOfOrNull { it.dateMillis }
                val maxDate = entries.maxOfOrNull { it.dateMillis }
                val spanDays = if (minDate != null && maxDate != null)
                    ((maxDate - minDate) / (1000.0 * 60 * 60 * 24)).toInt()
                else 0
                when {
                    spanDays > 35 -> entries.groupBy { entry ->
                        monthNames[localDateOf(entry.dateMillis).month - 1]
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

    private fun groupByDay(entries: List<ParsedEntry>): List<SpendingDataPoint> =
        entries.groupBy { entry ->
            val date = localDateOf(entry.dateMillis)
            "${date.day}/${date.month}"
        }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
            .sortedBy {
                val parts = it.label.split("/")
                (parts.getOrNull(1)?.toIntOrNull() ?: 0) * 100 + (parts.getOrNull(0)?.toIntOrNull() ?: 0)
            }

    private fun groupByWeek(entries: List<ParsedEntry>): List<SpendingDataPoint> =
        entries.groupBy { entry -> "Sem ${isoWeekOfYear(entry.dateMillis)}" }
            .map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
            .sortedBy { it.label.removePrefix("Sem ").toIntOrNull() ?: 0 }

    private fun parseEntry(entry: CsvEntry): ParsedEntry? {
        val dateMillis = parseDate(entry.date) ?: return null
        val amount = parseAmount(entry.amount) ?: return null
        return ParsedEntry(dateMillis, amount, entry.description, entry.category)
    }

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

    // Both parsers live in TransactionValueParser, shared with the automatic
    // importer, so the two import paths agree on what counts as a date and an
    // amount. It reads every shape this screen used to handle, plus the ones
    // an arbitrary statement can throw at it (named months, `1.234,56`,
    // parentheses for negatives).
    private fun parseDate(dateStr: String): Long? = TransactionValueParser.parseDate(dateStr)

    private fun parseAmount(amountStr: String): Double? = TransactionValueParser.parseAmount(amountStr)

    private companion object {
        const val ENTRIES_PAGE_SIZE = 30
    }
}
