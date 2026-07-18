package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalLime
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalOrange
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalRed
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PinkContainer
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

private val chartColors = listOf(
    BrutalPink,
    BrutalCyan,
    BrutalYellow,
    BrutalLime,
    BrutalOrange,
    BrutalRed,
    PinkContainer,
)

/**
 * Custom donut (doughnut) pie chart drawn on a Canvas with a legend,
 * colour-coded segments, and percentage labels.
 */
@Composable
fun SpendingPieChart(
    data: List<CategorySpending>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "R$",
    title: String? = null,
) {
    val total = data.sumOf { it.value }
    if (total <= 0.0 || data.isEmpty()) return

    val borderColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    HardShadowBox(
        offsetX = 5.dp,
        offsetY = 5.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .border(3.dp, borderColor, RectangleShape)
                .background(surfaceColor, RectangleShape)
                .padding(16.dp)
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title ?: stringResource(R.string.chart_spending_by_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(180.dp)
                ) {
                    val canvasSize = size
                    val diameter = minOf(canvasSize.width, canvasSize.height)
                    val topLeft = Offset(
                        (canvasSize.width - diameter) / 2f,
                        (canvasSize.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    data.forEachIndexed { index, item ->
                        val sweepAngle = if (index == data.lastIndex) {
                            360f - (startAngle + 90f)
                        } else {
                            ((item.value / total) * 360).toFloat().coerceAtLeast(0.5f)
                        }
                        drawArc(
                            color = chartColors[index % chartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = topLeft,
                            size = arcSize
                        )
                        startAngle += sweepAngle
                    }

                    drawCircle(
                        color = surfaceColor,
                        radius = diameter * 0.3f
                    )
                }

                Text(
                    text = "$currencySymbol ${total.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            data.forEachIndexed { index, item ->
                val pct = ((item.value / total) * 100).toInt()
                if (pct > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    chartColors[index % chartColors.size],
                                    RectangleShape
                                )
                                .border(1.5.dp, borderColor, RectangleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.category,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$pct%",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
}
