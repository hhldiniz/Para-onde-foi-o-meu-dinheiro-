package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground

/**
 * Full-screen overlay shown while files are being imported. Displays a spinner,
 * the currently importing file, and the list of already imported files.
 */
@Composable
fun ImportProgressOverlay(
    importingFileName: String?,
    importedFiles: List<String>,
    importingTotal: Int,
    modifier: Modifier = Modifier,
) {
    val currentIndex = importedFiles.size + 1
    val progressText = if (importingTotal > 0) {
        stringResource(
            R.string.import_loading_progress,
            currentIndex.coerceAtMost(importingTotal),
            importingTotal,
        )
    } else {
        ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrutalBlack.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        HardShadowBox(
            offsetX = 6.dp,
            offsetY = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBackground, RectangleShape)
                    .border(3.dp, BrutalBlack, RectangleShape)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.import_loading_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = BrutalBlack,
                    textAlign = TextAlign.Center,
                )

                Spacer8()

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .background(BrutalYellow, RectangleShape)
                        .border(3.dp, BrutalBlack, RectangleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    ImportSpinner()
                }

                Spacer8()

                if (importingFileName != null) {
                    Text(
                        text = stringResource(R.string.import_loading_current, importingFileName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrutalBlack,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.import_loading_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrutalBlack,
                        textAlign = TextAlign.Center,
                    )
                }

                if (progressText.isNotBlank()) {
                    Spacer8()
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = BrutalBlack.copy(alpha = 0.7f),
                    )
                }

                if (importedFiles.isNotEmpty()) {
                    Spacer16()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, BrutalBlack, RectangleShape)
                            .background(BrutalCyan.copy(alpha = 0.25f), RectangleShape)
                            .padding(12.dp),
                    ) {
                        importedFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.import_loading_done, file),
                                    tint = BrutalPink,
                                )
                                Text(
                                    text = file,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = BrutalBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A simple neo-brutalist spinner built with a rotating square. */
@Composable
private fun ImportSpinner() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
        ),
    )
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(22.dp)
            .rotate(rotation)
            .background(BrutalPink, RectangleShape)
            .border(2.dp, BrutalBlack, RectangleShape),
    )
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun Spacer16() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
}
