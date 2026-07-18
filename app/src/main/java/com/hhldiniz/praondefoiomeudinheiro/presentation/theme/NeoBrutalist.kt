package com.hhldiniz.praondefoiomeudinheiro.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun HardShadowBox(
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.15f),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        if (contentSize != IntSize.Zero) {
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(
                        with(density) { contentSize.width.toDp() },
                        with(density) { contentSize.height.toDp() }
                    )
                    .background(shadowColor, RectangleShape)
            )
        }
        Box(
            modifier = Modifier.onSizeChanged { contentSize = it }
        ) {
            content()
        }
    }
}

@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 3.dp,
    elevation: Dp = 6.dp,
    enabled: Boolean = true,
    text: String
) {
    val bg = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f)
    val border = if (enabled) borderColor else borderColor.copy(alpha = 0.3f)
    val txt = if (enabled) textColor else textColor.copy(alpha = 0.5f)

    HardShadowBox(
        offsetX = if (enabled) elevation else 0.dp,
        offsetY = if (enabled) elevation else 0.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, border, RectangleShape)
                .background(bg, RectangleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = txt,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 2.dp,
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    HardShadowBox(
        offsetX = elevation,
        offsetY = elevation,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, RectangleShape)
                .background(backgroundColor, RectangleShape)
                .padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun NeoTag(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onSecondary,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 2.dp
) {
    HardShadowBox(
        offsetX = 2.dp,
        offsetY = 2.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .border(borderWidth, borderColor, RectangleShape)
                .background(backgroundColor, RectangleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
