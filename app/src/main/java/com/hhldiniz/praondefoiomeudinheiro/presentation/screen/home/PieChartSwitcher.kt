package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hhldiniz.praondefoiomeudinheiro.R

/** Switches between the spending and earnings pie chart based on [showEarnings]. */
@Composable
fun PieChartSwitcher(
    categorySpending: List<CategorySpending>,
    categoryEarnings: List<CategorySpending>,
    currencySymbol: String,
    showEarnings: Boolean,
    modifier: Modifier = Modifier,
) {
    val data = if (showEarnings) categoryEarnings else categorySpending
    val title = if (showEarnings)
        stringResource(R.string.chart_earnings_by_category)
    else
        stringResource(R.string.chart_spending_by_category)

    SpendingPieChart(
        data = data,
        currencySymbol = currencySymbol,
        title = title,
        modifier = modifier
    )
}
