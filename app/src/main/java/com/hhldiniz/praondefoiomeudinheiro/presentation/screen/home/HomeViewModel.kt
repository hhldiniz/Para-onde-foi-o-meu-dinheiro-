package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hhldiniz.praondefoiomeudinheiro.PraondefoiomeudinheiroApp
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvUriHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class ParsedEntry(
    val dateMillis: Long,
    val amount: Double,
    val description: String,
    val category: String,
)

private data class LoadResult(
    val spending: List<ParsedEntry>,
    val earnings: List<ParsedEntry>,
    val errors: List<String>,
    val rawAmounts: List<String>,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileSpreadsheetRepository()
    private val app = application as PraondefoiomeudinheiroApp
    private val importRepository = app.importRepository
    private val _uiState = MutableStateFlow(HomeUiState(selectedCurrency = CurrencyHolder.selectedCurrency.value))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var rawSpending = listOf<ParsedEntry>()
    private var rawEarnings = listOf<ParsedEntry>()

    private val _entriesList = MutableStateFlow<List<EntryDisplay>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val entriesPagingData: Flow<PagingData<EntryDisplay>> = _entriesList
        .flatMapLatest { entries ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                EntriesPagingSource(entries)
            }.flow
        }

    init {
        viewModelScope.launch {
            CurrencyHolder.selectedCurrency.collect { currency ->
                _uiState.update { it.copy(selectedCurrency = currency) }
            }
        }
    }

    private val dateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()),
        SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
        SimpleDateFormat("d/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/M/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
    )

    fun loadData(contentResolver: ContentResolver) {
        val uriList = CsvUriHolder.uris
        if (rawSpending.isNotEmpty() || rawEarnings.isNotEmpty()) return
        if (uriList.isEmpty()) {
            viewModelScope.launch {
                val hasRoomData = withContext(Dispatchers.IO) { importRepository.count() > 0 }
                if (hasRoomData) {
                    loadFromRoom()
                } else {
                    _uiState.update { it.copy(debugMessage = "Nenhum dado encontrado. Usando dados mock.") }
                    loadMockData()
                }
            }
            return
        }
        viewModelScope.launch {
            val (spending, earnings, errors, rawAmounts) = withContext(Dispatchers.IO) {
                val s = mutableListOf<ParsedEntry>()
                val e = mutableListOf<ParsedEntry>()
                val errs = mutableListOf<String>()
                val amounts = mutableListOf<String>()
                val allImported = mutableListOf<ImportedEntry>()
                for (uri in uriList) {
                    repository.readValues(uri, contentResolver)
                        .onSuccess { range ->
                            range.spendingEntries.forEach { amounts.add(it.amount) }
                            range.earningsEntries.forEach { amounts.add(it.amount) }
                            val sp = range.spendingEntries.mapNotNull { parseEntry(it) }
                            val ep = range.earningsEntries.mapNotNull { parseEntry(it) }
                            s += sp
                            e += ep
                            val fileName = uri.lastPathSegment ?: ""
                            allImported += sp.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = true, fileName = fileName) }
                            allImported += ep.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = false, fileName = fileName) }
                            errs.add("CSV OK: ${sp.size} gastos, ${ep.size} rendas de ${range.spendingEntries.size + range.earningsEntries.size} linhas brutas")
                        }
                        .onFailure { error ->
                            errs.add("Falha ao ler $uri: ${error.message}")
                        }
                }
                importRepository.insertEntries(allImported)
                LoadResult(s, e, errs, amounts)
            }
            _uiState.update { it.copy(debugMessage = errors.joinToString("\n")) }
            if (spending.isEmpty() && earnings.isEmpty()) {
                _uiState.update { it.copy(debugMessage = (it.debugMessage ?: "") + "\nDados vazios. Usando mock.") }
                loadMockData()
            } else {
                rawSpending = spending
                rawEarnings = earnings
                val allCategories = (spending + earnings).map { it.category }.distinct().sorted()
                val minDate = (spending + earnings).minOfOrNull { it.dateMillis }
                val maxDate = (spending + earnings).maxOfOrNull { it.dateMillis }
                _uiState.update { it.copy(allCategories = allCategories, datasetMinDate = minDate, datasetMaxDate = maxDate) }
                val detected = rawAmounts.mapNotNull { CurrencyOption.fromAmountString(it) }
                if (detected.isNotEmpty()) {
                    val majority = detected.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
                    CurrencyHolder.setCurrency(majority)
                }
                loadDataForPeriod(Period.MONTH)
            }
        }
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { app.database.importedEntryDao().getAllEntriesByDate() }
            rawSpending = entries.filter { it.isExpense }.map { ParsedEntry(it.dateMillis, it.amount, it.description, it.category) }
            rawEarnings = entries.filter { !it.isExpense }.map { ParsedEntry(it.dateMillis, it.amount, it.description, it.category) }
            val allCategories = (rawSpending + rawEarnings).map { it.category }.distinct().sorted()
            val minDate = (rawSpending + rawEarnings).minOfOrNull { it.dateMillis }
            val maxDate = (rawSpending + rawEarnings).maxOfOrNull { it.dateMillis }
            _uiState.update { it.copy(allCategories = allCategories, datasetMinDate = minDate, datasetMaxDate = maxDate) }
            loadDataForPeriod(Period.MONTH)
        }
    }

    fun importFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val (imported, errors) = withContext(Dispatchers.IO) {
                val importedResult = mutableListOf<ImportedEntry>()
                val errorMessages = mutableListOf<String>()
                repository.readValues(uri, contentResolver)
                    .onSuccess { range ->
                        val fileName = uri.lastPathSegment ?: ""
                        val sp = range.spendingEntries.mapNotNull { parseEntry(it) }
                        val ep = range.earningsEntries.mapNotNull { parseEntry(it) }
                        val entries = sp.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = true, fileName = fileName) } +
                            ep.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = false, fileName = fileName) }
                        importedResult.addAll(entries)
                        importRepository.insertEntries(entries)
                        errorMessages.add("Importado: ${sp.size} gastos, ${ep.size} rendas")
                    }
                    .onFailure { error ->
                        errorMessages.add("Falha ao importar: ${error.message}")
                    }
                importedResult to errorMessages
            }
            val message = errors.joinToString("\n")
            _uiState.update { it.copy(debugMessage = message) }
            if (imported.isNotEmpty()) {
                rawSpending += imported.filter { it.isExpense }.map { ParsedEntry(it.dateMillis, it.amount, it.description, it.category) }
                rawEarnings += imported.filter { !it.isExpense }.map { ParsedEntry(it.dateMillis, it.amount, it.description, it.category) }
                val allCategories = (rawSpending + rawEarnings).map { it.category }.distinct().sorted()
                val minDate = (rawSpending + rawEarnings).minOfOrNull { it.dateMillis }
                val maxDate = (rawSpending + rawEarnings).maxOfOrNull { it.dateMillis }
                _uiState.update { it.copy(allCategories = allCategories, datasetMinDate = minDate, datasetMaxDate = maxDate) }
                loadDataForPeriod(_uiState.value.selectedPeriod)
            }
        }
    }

    fun importFolder(treeUri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            val (imported, errors) = withContext(Dispatchers.IO) {
                val csvUris = listCsvUris(context, treeUri)
                val importedResult = mutableListOf<ImportedEntry>()
                val errorMessages = mutableListOf<String>()
                for (uri in csvUris) {
                    repository.readValues(uri, context.contentResolver)
                        .onSuccess { range ->
                            val fileName = uri.lastPathSegment ?: ""
                            val sp = range.spendingEntries.mapNotNull { parseEntry(it) }
                            val ep = range.earningsEntries.mapNotNull { parseEntry(it) }
                            importedResult += sp.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = true, fileName = fileName) }
                            importedResult += ep.map { ImportedEntry(dateMillis = it.dateMillis, amount = it.amount, description = it.description, category = it.category, isExpense = false, fileName = fileName) }
                            errorMessages.add("${fileName}: ${sp.size} gastos, ${ep.size} rendas")
                        }
                        .onFailure { error ->
                            errorMessages.add("Falha ao ler $uri: ${error.message}")
                        }
                }
                importRepository.insertEntries(importedResult)
                importedResult to errorMessages
            }
            val message = errors.joinToString("\n")
            _uiState.update { it.copy(debugMessage = message) }
            if (imported.isNotEmpty()) {
                rawSpending += imported.filter { it.isExpense }.map { ParsedEntry(it.dateMillis, it.amount, it.description, it.category) }
                rawEarnings += imported.filter { !it.isExpense }.map { ParsedEntry(it.dateMillis, it.amount, it.description, it.category) }
                val allCategories = (rawSpending + rawEarnings).map { it.category }.distinct().sorted()
                val minDate = (rawSpending + rawEarnings).minOfOrNull { it.dateMillis }
                val maxDate = (rawSpending + rawEarnings).maxOfOrNull { it.dateMillis }
                _uiState.update { it.copy(allCategories = allCategories, datasetMinDate = minDate, datasetMaxDate = maxDate) }
                loadDataForPeriod(_uiState.value.selectedPeriod)
            }
        }
    }

    private fun listCsvUris(context: android.content.Context, treeUri: Uri): List<Uri> {
        val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return documentFile.listFiles()
            .filter { file ->
                val n = file.name
                n?.endsWith(".csv", ignoreCase = true) == true ||
                n?.endsWith(".ods", ignoreCase = true) == true
            }
            .mapNotNull { it.uri }
    }

    private fun loadMockData() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val monthNames = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
        val baseValues = listOf(1200.0, 980.0, 1500.0, 1100.0, 1350.0, 890.0, 1600.0, 1050.0, 1400.0, 1250.0, 950.0, 1800.0)
        val baseEarningsValues = listOf(10000.0, 10000.0, 10000.0, 10000.0, 10000.0, 10000.0, 10000.0, 10000.0, 10000.0, 10000.0, 10000.0, 10000.0)
        val categories = listOf("Alimentacao", "Transporte", "Lazer", "Saude", "Educacao", "Moradia", "Outros")
        val categoryRatios = listOf(0.232, 0.080, 0.186, 0.053, 0.066, 0.299, 0.084)
        val earningCategories = listOf("Salario", "Freelance", "Investimentos", "Outros")
        val earningRatios = listOf(0.80, 0.10, 0.07, 0.03)

        rawSpending = (0 until 12).map { i ->
            val cal = Calendar.getInstance().apply { set(year, i, 15, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            ParsedEntry(cal.timeInMillis, baseValues[i], "", categories[i % categories.size])
        }
        rawEarnings = (0 until 12).map { i ->
            val cal = Calendar.getInstance().apply { set(year, i, 5, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            ParsedEntry(cal.timeInMillis, baseEarningsValues[i], "", earningCategories[i % earningCategories.size])
        }
        val allCategories = (rawSpending + rawEarnings).map { it.category }.distinct().sorted()
        val minDate = (rawSpending + rawEarnings).minOfOrNull { it.dateMillis }
        val maxDate = (rawSpending + rawEarnings).maxOfOrNull { it.dateMillis }
        _uiState.update { it.copy(allCategories = allCategories, datasetMinDate = minDate, datasetMaxDate = maxDate) }
        loadDataForPeriod(Period.MONTH)
    }

    fun onPeriodSelected(period: Period) {
        if (period != _uiState.value.selectedPeriod) {
            if (period == Period.CUSTOM) {
                val endDate = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = endDate; add(Calendar.MONTH, -3) }
                filterByDateRange(cal.timeInMillis, endDate)
            } else {
                loadDataForPeriod(period)
            }
        }
    }

    fun onCustomDateRange(startDate: Long, endDate: Long) {
        filterByDateRange(startDate, endDate)
    }

    fun onPatrimonyChanged(value: Double) {
        _uiState.update { it.copy(patrimony = value) }
    }

    fun onCurrencyChanged(currency: CurrencyOption) {
        CurrencyHolder.setCurrency(currency)
    }

    fun onCategorySelected(category: String?) {
        val currentState = _uiState.value
        val period = currentState.selectedPeriod
        if (period == Period.CUSTOM) {
            filterAndEmit(
                period, currentState.customStartDate ?: 0L, currentState.customEndDate ?: 0L,
                currentState.customStartDate, currentState.customEndDate, category
            )
        } else {
            val allData = rawSpending + rawEarnings
            val minDate = allData.minOfOrNull { it.dateMillis }
            val maxDate = allData.maxOfOrNull { it.dateMillis }
            val (start, end) = if (minDate != null && maxDate != null) minDate to maxDate
            else Calendar.getInstance().run { timeInMillis to timeInMillis }
            filterAndEmit(period, start, end, null, null, category)
        }
    }

    private fun loadDataForPeriod(period: Period) {
        val allData = rawSpending + rawEarnings
        val minDate = allData.minOfOrNull { it.dateMillis }
        val maxDate = allData.maxOfOrNull { it.dateMillis }
        val (start, end) = if (minDate != null && maxDate != null) minDate to maxDate
        else Calendar.getInstance().run { timeInMillis to timeInMillis }
        filterAndEmit(period, start, end, null, null, _uiState.value.selectedCategory)
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
        selectedCategory: String? = _uiState.value.selectedCategory,
    ) {
        val monthNames = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
        val dayNames = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab")

        val dateFilteredSpending = rawSpending.filter { it.dateMillis in startMillis..endMillis }
        val dateFilteredEarnings = rawEarnings.filter { it.dateMillis in startMillis..endMillis }
        val filteredSpending = if (selectedCategory != null) dateFilteredSpending.filter { it.category == selectedCategory } else dateFilteredSpending
        val filteredEarnings = if (selectedCategory != null) dateFilteredEarnings.filter { it.category == selectedCategory } else dateFilteredEarnings

        val totalSpending = filteredSpending.sumOf { it.amount }
        val totalEarnings = filteredEarnings.sumOf { it.amount }

        val (spendingData, spendingCategories) = buildChartData(filteredSpending, period, monthNames, dayNames, totalSpending)
        val (earningsData, earningsCategories) = buildChartData(filteredEarnings, period, monthNames, dayNames, totalEarnings)

        val entries = (filteredSpending.map { EntryDisplay(it.dateMillis, it.description, it.category, it.amount, true) }
            + filteredEarnings.map { EntryDisplay(it.dateMillis, it.description, it.category, it.amount, false) })
            .sortedByDescending { it.dateMillis }

        _entriesList.value = entries

        _uiState.update {
            it.copy(
                spendingData = spendingData,
                categorySpending = spendingCategories,
                earningsData = earningsData,
                categoryEarnings = earningsCategories,
                selectedPeriod = period,
                totalSpending = totalSpending,
                totalEarnings = totalEarnings,
                customStartDate = customStart,
                customEndDate = customEnd,
                selectedCategory = selectedCategory,
            )
        }
    }

    private fun buildChartData(
        entries: List<ParsedEntry>,
        period: Period,
        monthNames: List<String>,
        dayNames: List<String>,
        total: Double,
    ): Pair<List<SpendingDataPoint>, List<CategorySpending>> {
        val lineData = when (period) {
            Period.DAY -> {
                entries.groupBy { entry ->
                    val cal = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
                    "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
                }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                    .sortedBy {
                        val parts = it.label.split("/")
                        (parts.getOrNull(1)?.toIntOrNull() ?: 0) * 100 + (parts.getOrNull(0)?.toIntOrNull() ?: 0)
                    }
            }
            Period.WEEK -> {
                entries.groupBy { entry ->
                    val cal = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
                    "Sem ${cal.get(Calendar.WEEK_OF_YEAR)}"
                }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                    .sortedBy { it.label.removePrefix("Sem ").toIntOrNull() ?: 0 }
            }
            Period.MONTH, Period.CUSTOM -> {
                val minDate = entries.minOfOrNull { it.dateMillis }
                val maxDate = entries.maxOfOrNull { it.dateMillis }
                val spanDays = if (minDate != null && maxDate != null)
                    ((maxDate - minDate) / (1000.0 * 60 * 60 * 24)).toInt()
                else 0
                when {
                    spanDays > 35 -> entries.groupBy { entry ->
                        val cal = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
                        monthNames[cal.get(Calendar.MONTH)]
                    }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                    spanDays > 7 -> entries.groupBy { entry ->
                        val cal = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
                        "Sem ${cal.get(Calendar.WEEK_OF_YEAR)}"
                    }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                        .sortedBy { it.label.removePrefix("Sem ").toIntOrNull() ?: 0 }
                    else -> entries.groupBy { entry ->
                        val cal = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
                        "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
                    }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                        .sortedBy {
                            val parts = it.label.split("/")
                            (parts.getOrNull(1)?.toIntOrNull() ?: 0) * 100 + (parts.getOrNull(0)?.toIntOrNull() ?: 0)
                        }
                }
            }
            Period.YEAR -> {
                entries.groupBy { entry ->
                    val cal = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
                    cal.get(Calendar.YEAR).toString()
                }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
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

    private fun parseEntry(entry: CsvEntry): ParsedEntry? {
        val dateMillis = parseDate(entry.date) ?: return null
        val amount = parseAmount(entry.amount) ?: return null
        return ParsedEntry(dateMillis, amount, entry.description, entry.category)
    }

    private fun parseDate(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        for (fmt in dateFormats) {
            try {
                val parsed = fmt.parse(trimmed)
                if (parsed != null) return parsed.time
            } catch (_: Exception) { }
        }
        return null
    }

    private fun parseAmount(amountStr: String): Double? {
        val cleaned = amountStr.trim()
            .replace("R$", "").replace("$", "").replace("€", "").replace("£", "")
            .trim()
        return if (cleaned.contains(",") && cleaned.contains(".")) {
            val dotLast = cleaned.lastIndexOf('.')
            val commaLast = cleaned.lastIndexOf(',')
            if (commaLast > dotLast) {
                cleaned.replace(".", "").replace(",", ".").toDoubleOrNull()
            } else {
                cleaned.replace(",", "").toDoubleOrNull()
            }
        } else if (cleaned.contains(",")) {
            cleaned.replace(",", ".").toDoubleOrNull()
        } else {
            cleaned.toDoubleOrNull()
        }
    }
}

private class EntriesPagingSource(
    private val entries: List<EntryDisplay>,
) : PagingSource<Int, EntryDisplay>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EntryDisplay> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, entries.size)
        if (startIndex >= entries.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
            )
        }
        return LoadResult.Page(
            data = entries.subList(startIndex, endIndex),
            prevKey = if (page > 0) page - 1 else null,
            nextKey = if (endIndex < entries.size) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, EntryDisplay>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
