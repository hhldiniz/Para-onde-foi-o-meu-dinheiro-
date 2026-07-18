package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalRed
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoButton
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoCard
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoTag
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PraOndeFoiOMeuDinheiroTheme
import androidx.compose.ui.res.stringResource

/**
 * Landing screen that guides the user through selecting spreadsheet files
 * or folders, validating them, and proceeding to the Home screen.
 */
@Composable
fun LandingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LandingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onFilePicked(uri, context)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onFolderPicked(uri, context)
    }

    when (val state = uiState) {
        is LandingUiState.Idle -> {
            LandingContent(
                onPickFile = {
                    filePickerLauncher.launch(arrayOf("text/*", "*/*"))
                },
                onPickFolder = {
                    folderPickerLauncher.launch(null)
                },
                modifier = modifier
            )
        }

        is LandingUiState.Loading -> {
            LoadingContent(modifier = modifier)
        }

        is LandingUiState.ValidationResult -> {
            ValidationResultContent(
                report = state.report,
                onContinue = { viewModel.onContinue() },
                onBack = { viewModel.onReset() },
                modifier = modifier
            )
        }

        is LandingUiState.Error -> {
            ErrorContent(
                message = state.message,
                onBack = { viewModel.onReset() },
                modifier = modifier
            )
        }

        is LandingUiState.ProceedToHome -> {
            onComplete()
        }
    }
}

/** Initial landing UI with title, instruction text and file/folder selection buttons. */
@Composable
private fun LandingContent(
    onPickFile: () -> Unit,
    onPickFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
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
                    text = stringResource(R.string.landing_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.landing_instruction),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(48.dp))

        NeoButton(
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            textColor = MaterialTheme.colorScheme.onPrimary,
            text = stringResource(R.string.landing_btn_select_csv)
        )

        Spacer(modifier = Modifier.height(16.dp))

        NeoButton(
            onClick = onPickFolder,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalCyan,
            textColor = MaterialTheme.colorScheme.onTertiary,
            text = stringResource(R.string.landing_btn_select_folder)
        )
    }
}

/** Loading indicator shown while files are being validated. */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.landing_validating),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** Displays the validation result with lists of valid/invalid files and continue/back actions. */
@Composable
private fun ValidationResultContent(
    report: FileValidationReport,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        HardShadowBox(
            offsetX = 5.dp,
            offsetY = 5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(BrutalPink, RectangleShape)
                    .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (report.hasInvalidFiles) stringResource(R.string.landing_title_invalid)
                           else stringResource(R.string.landing_title_valid),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (report.hasInvalidFiles && report.hasValidFiles) {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalYellow,
                borderColor = MaterialTheme.colorScheme.outline,
                borderWidth = 3.dp,
                elevation = 4.dp
            ) {
                Text(
                    text = stringResource(R.string.landing_warning_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrutalBlack
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (report.validFiles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    NeoTag(
                        text = stringResource(R.string.landing_tag_valid, report.validFiles.size),
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                        borderColor = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(report.validFiles) { file ->
                    ValidFileCard(file)
                }
            }

            if (report.invalidFiles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    NeoTag(
                        text = stringResource(R.string.landing_tag_ignored, report.invalidFiles.size),
                        backgroundColor = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.onError,
                        borderColor = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(report.invalidFiles) { file ->
                    InvalidFileCard(file)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeoButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            textColor = MaterialTheme.colorScheme.onPrimary,
            text = stringResource(R.string.landing_btn_continue),
            enabled = report.hasValidFiles
        )

        Spacer(modifier = Modifier.height(8.dp))

        NeoButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalCyan,
            textColor = MaterialTheme.colorScheme.onTertiary,
            text = stringResource(R.string.landing_btn_back)
        )
    }
}

/** Card showing a valid file's name and detected columns. */
@Composable
private fun ValidFileCard(file: ValidSpreadsheetFile) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        borderWidth = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardShadowBox(
                offsetX = 2.dp,
                offsetY = 2.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrutalCyan, RectangleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2713",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.landing_columns_format, file.headerColumns.size,
                        file.headerColumns.joinToString(", ")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Card showing an invalid file's name and the reason it was rejected. */
@Composable
private fun InvalidFileCard(file: InvalidSpreadsheetFile) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = BrutalRed,
        borderColor = MaterialTheme.colorScheme.outline,
        borderWidth = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardShadowBox(
                offsetX = 2.dp,
                offsetY = 2.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error, RectangleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
                Text(
                    text = file.reason,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

/** Full-screen error state with an icon, message and back button. */
@Composable
private fun ErrorContent(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HardShadowBox(
            offsetX = 4.dp,
            offsetY = 4.dp,
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, RectangleShape)
                    .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline,
            borderWidth = 3.dp
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        NeoButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            textColor = MaterialTheme.colorScheme.onPrimary,
            text = stringResource(R.string.landing_btn_back)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LandingContentPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        LandingContent(
            onPickFile = {},
            onPickFolder = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingContentPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        LoadingContent()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ErrorContentPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        ErrorContent(
            message = "Nenhum arquivo .csv válido encontrado na pasta.",
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ValidationResultMixedPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        ValidationResultContent(
            report = FileValidationReport(
                validFiles = listOf(
                    ValidSpreadsheetFile("gastos.csv", Uri.EMPTY, listOf("Data", "Valor", "Descrição")),
                    ValidSpreadsheetFile("receitas.csv", Uri.EMPTY, listOf("Data", "Valor"))
                ),
                invalidFiles = listOf(
                    InvalidSpreadsheetFile("notas.txt", Uri.EMPTY, "Formato não suportado. Apenas arquivos .csv e .ods são aceitos."),
                    InvalidSpreadsheetFile("vazio.csv", Uri.EMPTY, "Arquivo vazio.")
                )
            ),
            onContinue = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ValidationResultAllValidPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        ValidationResultContent(
            report = FileValidationReport(
                validFiles = listOf(
                    ValidSpreadsheetFile("janeiro.csv", Uri.EMPTY, listOf("Data", "Descrição", "Valor", "Categoria")),
                    ValidSpreadsheetFile("fevereiro.csv", Uri.EMPTY, listOf("Data", "Descrição", "Valor", "Categoria")),
                    ValidSpreadsheetFile("marco.csv", Uri.EMPTY, listOf("Data", "Descrição", "Valor", "Categoria"))
                ),
                invalidFiles = emptyList()
            ),
            onContinue = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ValidationResultAllInvalidPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        ValidationResultContent(
            report = FileValidationReport(
                validFiles = emptyList(),
                invalidFiles = listOf(
                    InvalidSpreadsheetFile("foto.jpg", Uri.EMPTY, "Formato não suportado. Apenas arquivos .csv e .ods são aceitos."),
                    InvalidSpreadsheetFile("documento.pdf", Uri.EMPTY, "Formato não suportado. Apenas arquivos .csv e .ods são aceitos.")
                )
            ),
            onContinue = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ValidFileCardPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        ValidFileCard(
            file = ValidSpreadsheetFile(
                name = "gastos_2024.ods",
                uri = Uri.EMPTY,
                headerColumns = listOf("Data", "Descrição", "Valor", "Categoria")
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvalidFileCardPreview() {
    PraOndeFoiOMeuDinheiroTheme {
        InvalidFileCard(
            file = InvalidSpreadsheetFile(
                name = "arquivo_errado.xlsx",
                uri = Uri.EMPTY,
                reason = "Formato não suportado. Apenas arquivos .csv e .ods são aceitos."
            )
        )
    }
}
