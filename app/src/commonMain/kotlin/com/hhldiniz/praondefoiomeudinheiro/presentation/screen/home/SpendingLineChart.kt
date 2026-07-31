package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox

/**
 * Custom line chart drawn on a Canvas with grid lines, value labels,
 * filled area underneath the line, and data-point markers.
 *
 * Axis labels are drawn with Compose's own [rememberTextMeasurer] rather than
 * a platform canvas, so the chart renders identically on Android and iOS.
 */
@Composable
fun SpendingLineChart(
    data: List<SpendingDataPoint>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "R$",
    lineColor: Color = BrutalPink,
) {
    if (data.isEmpty()) return

    val borderColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    HardShadowBox(
        offsetX = 5.dp,
        offsetY = 5.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .border(3.dp, borderColor, RectangleShape)
                .background(surfaceColor, RectangleShape)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(start = 60.dp, end = 16.dp, top = 16.dp, bottom = 44.dp)
            ) {
                val axisStyle = TextStyle(
                    color = onSurface,
                    fontSize = 30f.toSp(),
                    fontWeight = FontWeight.Bold,
                )
                val labelStyle = TextStyle(
                    color = onSurface,
                    fontSize = 27f.toSp(),
                    fontWeight = FontWeight.Bold,
                )

                val maxValue = data.maxOf { it.value }
                val gridLines = 4
                val stepX = if (data.size > 1) size.width / (data.size - 1) else size.width

                val points = data.mapIndexed { index, point ->
                    val x = if (data.size > 1) stepX * index else size.width / 2f
                    val y = size.height - ((point.value / maxValue) * size.height).toFloat()
                    Offset(x, y)
                }

                for (i in 0..gridLines) {
                    val y = size.height * i / gridLines
                    drawLine(
                        color = lineColor.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                    // Right-aligned against the chart's left edge.
                    val layout = textMeasurer.measure(
                        "$currencySymbol ${(maxValue - (maxValue * i / gridLines)).toInt()}",
                        axisStyle,
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(-8f - layout.size.width, y - layout.size.height / 2f),
                    )
                }

                if (data.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, size.height)
                        lineTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                        lineTo(points.last().x, size.height)
                        close()
                    }
                    drawPath(fillPath, lineColor.copy(alpha = 0.12f))

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = lineColor,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                points.forEachIndexed { index, point ->
                    val rectSize = if (data.size > 1) 10f else 24f
                    drawRect(
                        color = lineColor,
                        topLeft = Offset(point.x - rectSize / 2f, point.y - rectSize / 2f),
                        size = Size(rectSize, rectSize)
                    )
                    // Centred underneath its data point.
                    val layout = textMeasurer.measure(data[index].label, labelStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(point.x - layout.size.width / 2f, size.height + 12f),
                    )
                }
            }
        }
    }
}
