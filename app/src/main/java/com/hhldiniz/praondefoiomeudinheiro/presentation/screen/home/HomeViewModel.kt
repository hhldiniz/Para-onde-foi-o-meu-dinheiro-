package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvUriHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

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

private fun ImportedEntry.toEntryDisplay() = EntryDisplay(dateMillis, description, category, amount, isExpense)

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
) : ViewModel() {

    private suspend fun saveCategoriesFromEntries(entries: List<ImportedEntry>): Int {
        val existing = categoryRepository.getAllSync().map { it.name }.toSet()
        val toInsert = deriveCategoriesToInsert(entries, existing)
        toInsert.forEach { categoryRepository.insert(it) }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val entriesPagingData: Flow<PagingData<EntryDisplay>> = _filterParams
        .flatMapLatest { params ->
            flow {
                val entries = withContext(Dispatchers.IO) {
                    val spending = importRepository.getEntriesByDateRange(
                        isExpense = true, category = params.selectedCategory,
                        startMillis = params.startMillis, endMillis = params.endMillis,
                    )
                    val earnings = importRepository.getEntriesByDateRange(
                        isExpense = false, category = params.selectedCategory,
                        startMillis = params.startMillis, endMillis = params.endMillis,
                    )
                    (spending + earnings).sortedByDescending { it.dateMillis }
                }
                emit(PagingData.from(entries.map { it.toEntryDisplay() }))
            }
        }

    private val zoneId = ZoneId.systemDefault()
    private val weekFields = WeekFields.of(Locale.getDefault())

    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd/M/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
    )

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
    }

    fun loadData(contentResolver: ContentResolver) {
        val uriList = CsvUriHolder.uris
        if (uriList.isEmpty()) {
            viewModelScope.launch {
                val hasRoomData = withContext(Dispatchers.IO) { importRepository.count() > 0 }
                if (hasRoomData && !DataClearedHolder.cleared.value) {
                    loadFromRoom()
                } else {
                    showZeroedState()
                }
            }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
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
                            s.addAll(sp)
                            e.addAll(ep)
                            val fileName = uri.lastPathSegment ?: ""
                            sp.forEach { parsed ->
                                allImported.add(ImportedEntry(dateMillis = parsed.dateMillis, amount = parsed.amount, description = parsed.description, category = parsed.category, isExpense = true, fileName = fileName))
                            }
                            ep.forEach { parsed ->
                                allImported.add(ImportedEntry(dateMillis = parsed.dateMillis, amount = parsed.amount, description = parsed.description, category = parsed.category, isExpense = false, fileName = fileName))
                            }
                            errs.add("CSV OK: ${sp.size} gastos, ${ep.size} rendas de ${range.spendingEntries.size + range.earningsEntries.size} linhas brutas")
                        }
                        .onFailure { error ->
                            errs.add("Falha ao ler $uri: ${error.message}")
                        }
                }
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
        }
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            updateDerivedState()
            loadDataForPeriod(Period.MONTH)
        }
    }

    fun importFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val (imported, errors, rawAmounts) = withContext(Dispatchers.IO) {
                val errorMessages = mutableListOf<String>()
                val raw = mutableListOf<String>()
                val result = repository.readValues(uri, contentResolver)
                if (result.isSuccess) {
                    val range = result.getOrThrow()
                    range.spendingEntries.forEach { raw.add(it.amount) }
                    range.earningsEntries.forEach { raw.add(it.amount) }
                    val fileName = uri.lastPathSegment ?: ""
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
        }
    }

    fun importFolder(treeUri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            val (imported, errors) = withContext(Dispatchers.IO) {
                val csvUris = listCsvUris(context, treeUri)
                val allEntries = mutableListOf<ImportedEntry>()
                val errorMessages = mutableListOf<String>()
                val raw = mutableListOf<String>()
                for (uri in csvUris) {
                    val result = repository.readValues(uri, context.contentResolver)
                    if (result.isSuccess) {
                        val range = result.getOrThrow()
                        val fileName = uri.lastPathSegment ?: ""
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
                        errorMessages.add("${fileName}: ${sp.size} gastos, ${ep.size} rendas")
                    } else {
                        val error = result.exceptionOrNull()
                        errorMessages.add("Falha ao ler $uri: ${error?.message}")
                    }
                }
                val inserted = importRepository.insertEntries(allEntries)
                val newCategories = saveCategoriesFromEntries(allEntries)
                errorMessages.add("Total: ${inserted.size} registros (${allEntries.size - inserted.size} duplicatas ignoradas)")
                if (newCategories > 0) {
                    errorMessages.add("$newCategories categoria(s) nova(s) salva(s) do arquivo")
                }
                detectCurrency(raw)
                inserted to errorMessages
            }
            _uiState.update { it.copy(debugMessage = errors.joinToString("\n")) }
            if (imported.isNotEmpty()) {
                DataClearedHolder.reset()
                updateDerivedState()
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

    fun refreshData() {
        viewModelScope.launch {
            val hasRoomData = withContext(Dispatchers.IO) { importRepository.count() > 0 }
            if (hasRoomData && !DataClearedHolder.cleared.value) {
                loadFromRoom()
            } else {
                showZeroedState()
            }
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
                kPeriod = period,
                startMillis = currentState.customStartDate ?: 0L,
                endMillis = currentState.customEndDate ?: 0L,
                customStart = currentState.customStartDate,
                customEnd = currentState.customEndDate,
                selectedCategory = category,
            )
        } else {
            viewModelScope.launch {
                val (start, end) = withContext(Dispatchers.IO) {
                    val min = importRepository.getMinDate() ?: System.currentTimeMillis()
                    val max = importRepository.getMaxDate() ?: min
                    min to max
                }
                filterAndEmit(period, start, end, null, null, category)
            }
        }
    }

    private fun updateDerivedState() {
        viewModelScope.launch {
            val (allCategories, minDate, maxDate) = withContext(Dispatchers.IO) {
                val categories = importRepository.getCategoryTotals(
                    isExpense = true, category = null,
                    startMillis = Long.MIN_VALUE, endMillis = Long.MAX_VALUE,
                ).map { it.category } +
                    importRepository.getCategoryTotals(
                        isExpense = false, category = null,
                        startMillis = Long.MIN_VALUE, endMillis = Long.MAX_VALUE,
                    ).map { it.category }
                val minDate = importRepository.getMinDate()
                val maxDate = importRepository.getMaxDate()
                Triple(categories.distinct().sorted(), minDate, maxDate)
            }
            _uiState.update { it.copy(allCategories = allCategories, datasetMinDate = minDate, datasetMaxDate = maxDate) }
        }
    }

    private fun loadDataForPeriod(period: Period) {
        viewModelScope.launch {
            val (start, end) = withContext(Dispatchers.IO) {
                val min = importRepository.getMinDate() ?: System.currentTimeMillis()
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
        viewModelScope.launch {
            val (spendingData, spendingCategories, earningsData, earningsCategories, totalSpending, totalEarnings) =
                withContext(Dispatchers.IO) {
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
            Period.DAY -> {
                entries.groupBy { entry ->
                    val zdt = Instant.ofEpochMilli(entry.dateMillis).atZone(zoneId)
                    "${zdt.dayOfMonth}/${zdt.monthValue}"
                }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                    .sortedBy {
                        val parts = it.label.split("/")
                        (parts.getOrNull(1)?.toIntOrNull() ?: 0) * 100 + (parts.getOrNull(0)?.toIntOrNull() ?: 0)
                    }
            }
            Period.WEEK -> {
                entries.groupBy { entry ->
                    val zdt = Instant.ofEpochMilli(entry.dateMillis).atZone(zoneId)
                    "Sem ${zdt.get(weekFields.weekOfYear())}"
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
                        val zdt = Instant.ofEpochMilli(entry.dateMillis).atZone(zoneId)
                        monthNames[zdt.monthValue - 1]
                    }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                    spanDays > 7 -> entries.groupBy { entry ->
                        val zdt = Instant.ofEpochMilli(entry.dateMillis).atZone(zoneId)
                        "Sem ${zdt.get(weekFields.weekOfYear())}"
                    }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                        .sortedBy { it.label.removePrefix("Sem ").toIntOrNull() ?: 0 }
                    else -> entries.groupBy { entry ->
                        val zdt = Instant.ofEpochMilli(entry.dateMillis).atZone(zoneId)
                        "${zdt.dayOfMonth}/${zdt.monthValue}"
                    }.map { (label, list) -> SpendingDataPoint(label, list.sumOf { it.amount }) }
                        .sortedBy {
                            val parts = it.label.split("/")
                            (parts.getOrNull(1)?.toIntOrNull() ?: 0) * 100 + (parts.getOrNull(0)?.toIntOrNull() ?: 0)
                        }
                }
            }
            Period.YEAR -> {
                entries.groupBy { entry ->
                    val zdt = Instant.ofEpochMilli(entry.dateMillis).atZone(zoneId)
                    zdt.year.toString()
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
            val locale = Locale.getDefault()
            val country = locale.country
            CurrencyHolder.setCurrency(
                if (country == "AR") CurrencyOption.ARS else CurrencyOption.BRL
            )
        } else if (dotCount > commaCount && dotCount > rawAmounts.size / 2) {
            CurrencyHolder.setCurrency(CurrencyOption.USD)
        }
    }

    private fun parseDate(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        for (fmt in dateFormats) {
            try {
                val localDate = java.time.LocalDate.parse(trimmed, fmt)
                return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            } catch (_: Exception) { }
        }
        return null
    }

    private fun parseAmount(amountStr: String): Double? {
        val cleaned = amountStr.trim()
            .replace(Regex("R\\$|[€£\\$]"), "")
            .trim()
        return when {
            cleaned.contains(",") && cleaned.contains(".") -> {
                val dotLast = cleaned.lastIndexOf('.')
                val commaLast = cleaned.lastIndexOf(',')
                if (commaLast > dotLast) {
                    cleaned.replace(".", "").replace(",", ".").toDoubleOrNull()
                } else {
                    cleaned.replace(",", "").toDoubleOrNull()
                }
            }
            cleaned.contains(",") -> cleaned.replace(",", ".").toDoubleOrNull()
            else -> cleaned.toDoubleOrNull()
        }
    }
}
