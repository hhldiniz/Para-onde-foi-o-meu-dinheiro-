package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox

@Composable
fun SpendingLineChart(
    data: List<SpendingDataPoint>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "R$",
    lineColor: androidx.compose.ui.graphics.Color = BrutalPink,
) {
    if (data.isEmpty()) return

    val borderColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textColor = onSurface.toArgb()

    val textPaint = remember(textColor) {
        android.graphics.Paint().apply {
            color = textColor
            textSize = 30f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.RIGHT
        }
    }

    val labelPaint = remember(textColor) {
        android.graphics.Paint().apply {
            color = textColor
            textSize = 27f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

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
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "$currencySymbol ${(maxValue - (maxValue * i / gridLines)).toInt()}",
                        -8f,
                        y + 10f,
                        textPaint
                    )
                }
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
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        data[index].label,
                        point.x,
                        size.height + 34f,
                        labelPaint
                    )
                }
            }
        }
    }
}
}
