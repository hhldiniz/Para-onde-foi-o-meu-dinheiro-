package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.annotation.StringRes
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption

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
    val patrimony: Double = 100000.0,
    val selectedCurrency: CurrencyOption = CurrencyOption.BRL,
    val debugMessage: String? = null,
    val allCategories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val datasetMinDate: Long? = null,
    val datasetMaxDate: Long? = null,
)

data class SpendingDataPoint(
    val label: String,
    val value: Double
)

data class CategorySpending(
    val category: String,
    val value: Double
)

enum class Period(@StringRes val labelRes: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    YEAR(R.string.period_year),
    CUSTOM(R.string.period_custom)
}

data class EntryDisplay(
    val dateMillis: Long,
    val description: String,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
)