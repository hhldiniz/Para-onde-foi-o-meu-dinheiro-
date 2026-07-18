package com.hhldiniz.praondefoiomeudinheiro.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** All shape corners are set to 0dp to achieve the sharp neo-brutalist look. */
private val NeoShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = BrutalPink,
    onPrimary = BrutalWhite,
    primaryContainer = PinkContainer,
    onPrimaryContainer = BrutalBlack,
    secondary = BrutalYellow,
    onSecondary = BrutalBlack,
    secondaryContainer = YellowContainer,
    onSecondaryContainer = BrutalBlack,
    tertiary = BrutalCyan,
    onTertiary = BrutalBlack,
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = BrutalBlack,
    error = BrutalRed,
    onError = BrutalWhite,
    errorContainer = RedContainer,
    onErrorContainer = BrutalBlack,
    background = LightBackground,
    onBackground = BrutalBlack,
    surface = LightSurface,
    onSurface = BrutalBlack,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF4A4A4A),
    outline = BrutalBlack,
    outlineVariant = Color(0xFFCCCCCC)
)

/**
 * App-wide Material 3 theme wrapping the provided content with the
 * neo-brutalist colour scheme, typography and zero-rounded shapes.
 */
@Composable
fun PraOndeFoiOMeuDinheiroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = NeoShapes,
        content = content
    )
}
