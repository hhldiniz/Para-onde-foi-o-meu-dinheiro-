package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import org.jetbrains.compose.resources.StringResource
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.period_custom
import com.hhldiniz.praondefoiomeudinheiro.resources.period_day
import com.hhldiniz.praondefoiomeudinheiro.resources.period_month
import com.hhldiniz.praondefoiomeudinheiro.resources.period_week
import com.hhldiniz.praondefoiomeudinheiro.resources.period_year

/** UI state for the Home screen, aggregating chart data, filters, and metadata. */
data class HomeUiState(
    val spendingData: List<SpendingDataPoint> = emptyList(),
    val categorySpending: List<CategorySpending> = emptyList(),
    val earningsData: List<SpendingDataPoint> = emptyList(),
    val categoryEarnings: List<CategorySpending> = emptyList(),
    val selectedPeriod: Period = Period.MONTH,
    val totalSpending: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val patrimony: Double = 0.0,
    val selectedCurrency: CurrencyOption = CurrencyOption.BRL,
    val debugMessage: String? = null,
    val allCategories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val datasetMinDate: Long? = null,
    val datasetMaxDate: Long? = null,
    val isImporting: Boolean = false,
    val importingFileName: String? = null,
    val importedFiles: List<String> = emptyList(),
    val importingTotal: Int = 0,
)

/** A single data point for line/bar charts, with a display label and numeric value. */
data class SpendingDataPoint(
    val label: String,
    val value: Double
)

/** Spending/earnings amount grouped by category for pie chart rendering. */
data class CategorySpending(
    val category: String,
    val value: Double
)

/** Time period options for filtering chart data and entry lists. */
enum class Period(val labelRes: StringResource) {
    DAY(Res.string.period_day),
    WEEK(Res.string.period_week),
    MONTH(Res.string.period_month),
    YEAR(Res.string.period_year),
    CUSTOM(Res.string.period_custom)
}

/**
 * The Home screen's bottom-bar tabs. [INVESTMENTS] renders
 * `InvestmentsScreen`, which has its own ViewModel and its own add button —
 * Home's import FAB only makes sense on [SUMMARY].
 */
enum class HomeTab {
    SUMMARY,
    ENTRIES,
    INVESTMENTS,
}

/** A single entry suitable for display in the entries list. */
data class EntryDisplay(
    val dateMillis: Long,
    val description: String,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
    val id: Long = 0L,
)