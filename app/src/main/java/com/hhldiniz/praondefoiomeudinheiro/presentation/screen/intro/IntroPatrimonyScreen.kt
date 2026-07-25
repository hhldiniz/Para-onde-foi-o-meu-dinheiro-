package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoButton
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PraOndeFoiOMeuDinheiroTheme
import org.koin.androidx.compose.koinViewModel

/**
 * First onboarding screen: asks the user for the patrimony (net worth) they
 * want to start tracking from.
 */
@Composable
fun IntroPatrimonyScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntroPatrimonyViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.confirmed) {
        onContinue()
        return
    }

    IntroPatrimonyContent(
        amountText = uiState.amountText,
        onAmountChanged = viewModel::onAmountChanged,
        onContinue = viewModel::onContinue,
        modifier = modifier,
    )
}

@Composable
private fun IntroPatrimonyContent(
    amountText: String,
    onAmountChanged: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HardShadowBox(
                offsetX = 6.dp,
                offsetY = 6.dp,
            ) {
                Box(
                    modifier = Modifier
                        .background(BrutalYellow, RectangleShape)
                        .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.intro_patrimony_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.intro_patrimony_instruction),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(32.dp))

        HardShadowBox(
            offsetX = 4.dp,
            offsetY = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextField(
                value = amountText,
                onValueChange = onAmountChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.intro_patrimony_amount_placeholder),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = BrutalBlack,
                    textAlign = TextAlign.Center,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LightBackground,
                    unfocusedContainerColor = LightBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = BrutalBlack,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        NeoButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            textColor = MaterialTheme.colorScheme.onPrimary,
            text = stringResource(R.string.intro_patrimony_btn_continue),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun IntroPatrimonyContentPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        IntroPatrimonyContent(
            amountText = "",
            onAmountChanged = {},
            onContinue = {},
        )
    }
}
