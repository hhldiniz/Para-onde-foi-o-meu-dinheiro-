package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink

/** Switches between the spending and earnings line chart based on [showEarnings]. */
@Composable
fun LineChartSwitcher(
    spendingData: List<SpendingDataPoint>,
    earningsData: List<SpendingDataPoint>,
    currencySymbol: String,
    showEarnings: Boolean,
    modifier: Modifier = Modifier,
) {
    val data = if (showEarnings) earningsData else spendingData
    val lineColor = if (showEarnings) BrutalCyan else BrutalPink

    SpendingLineChart(
        data = data,
        currencySymbol = currencySymbol,
        lineColor = lineColor,
        modifier = modifier
    )
}
