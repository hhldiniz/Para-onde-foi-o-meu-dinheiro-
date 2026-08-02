package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.domain.model.resolve
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.FieldMapping
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportSource
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.TransactionField
import com.hhldiniz.praondefoiomeudinheiro.platform.currencyFormatter
import com.hhldiniz.praondefoiomeudinheiro.platform.rememberImportSourcePicker
import com.hhldiniz.praondefoiomeudinheiro.presentation.components.localizedCategoryName
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoButton
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoCard
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoTag
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.action_back
import com.hhldiniz.praondefoiomeudinheiro.resources.entry_type_expense
import com.hhldiniz.praondefoiomeudinheiro.resources.entry_type_income
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_analyzing
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_analyzing_hint
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_btn_done
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_btn_import
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_btn_pick
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_btn_pick_another
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_done_categories
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_done_summary
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_done_title
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_field_amount
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_field_category
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_field_credit
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_field_date
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_field_description
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_field_type
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_intro
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_low_confidence
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_mapping_item
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_mapping_title
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_no_category
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_review_title
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_csv
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_image
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_ods
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_pdf
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_summary
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_title
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_toggle_hint
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * The automatic import screen: pick anything (photo, screenshot, spreadsheet,
 * PDF), see which columns the classifier recognized and which transactions it
 * proposes, then confirm. The direct import on the Home screen is untouched
 * and stays the exact path for files already in the expected format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartImportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmartImportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberImportSourcePicker { file -> viewModel.onFilePicked(file) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.smart_import_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            when (uiState.stage) {
                SmartImportStage.IDLE -> IdleContent(
                    error = uiState.error?.resolve(),
                    onPick = { picker.launch() },
                )

                SmartImportStage.ANALYZING -> AnalyzingContent(fileName = uiState.fileName)

                SmartImportStage.REVIEW, SmartImportStage.SAVING -> ReviewContent(
                    state = uiState,
                    saving = uiState.stage == SmartImportStage.SAVING,
                    onToggle = viewModel::onCandidateToggled,
                    onToggleType = viewModel::onCandidateTypeToggled,
                    onConfirm = viewModel::confirmImport,
                    onPickAnother = {
                        viewModel.reset()
                        picker.launch()
                    },
                )

                SmartImportStage.DONE -> DoneContent(
                    state = uiState,
                    onFinish = onNavigateBack,
                )
            }
        }
    }
}

@Composable
private fun IdleContent(error: String?, onPick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeoCard(backgroundColor = BrutalYellow) {
            Text(
                text = stringResource(Res.string.smart_import_intro),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            NeoCard(backgroundColor = BrutalPink) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        NeoButton(
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalCyan,
            textColor = MaterialTheme.colorScheme.onTertiary,
            text = stringResource(Res.string.smart_import_btn_pick),
        )
    }
}

@Composable
private fun AnalyzingContent(fileName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.smart_import_analyzing, fileName),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.smart_import_analyzing_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReviewContent(
    state: SmartImportUiState,
    saving: Boolean,
    onToggle: (Int) -> Unit,
    onToggleType: (Int) -> Unit,
    onConfirm: () -> Unit,
    onPickAnother: () -> Unit,
) {
    val currency by CurrencyHolder.selectedCurrency.collectAsStateWithLifecycle()
    val format = remember(currency) { currencyFormatter(currency) }

    Column(modifier = Modifier.fillMaxSize()) {
        NeoCard {
            Column {
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        Res.string.smart_import_summary,
                        state.rowsScanned,
                        (state.confidence * 100).roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.smart_import_mapping_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.source?.let { source -> NeoTag(text = sourceLabel(source), backgroundColor = BrutalCyan) }
                    state.mappings.forEach { mapping -> MappingTag(mapping) }
                }
                if (state.isLowConfidence) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.smart_import_low_confidence),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.smart_import_review_title, state.candidates.size),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(Res.string.smart_import_toggle_hint),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.candidates) { index, candidate ->
                CandidateRow(
                    candidate = candidate,
                    amountText = format.format(candidate.transaction.amount),
                    onToggle = { onToggle(index) },
                    onToggleType = { onToggleType(index) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        NeoButton(
            onClick = onPickAnother,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = LightBackground,
            textColor = MaterialTheme.colorScheme.onBackground,
            enabled = !saving,
            text = stringResource(Res.string.smart_import_btn_pick_another),
        )

        Spacer(Modifier.height(8.dp))

        NeoButton(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalCyan,
            textColor = MaterialTheme.colorScheme.onTertiary,
            enabled = !saving && state.selectedCount > 0,
            text = stringResource(Res.string.smart_import_btn_import, state.selectedCount),
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CandidateRow(
    candidate: TransactionCandidate,
    amountText: String,
    onToggle: () -> Unit,
    onToggleType: () -> Unit,
) {
    val transaction = candidate.transaction
    NeoCard(
        backgroundColor = if (candidate.selected) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionBox(selected = candidate.selected, onClick = onToggle)

            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)) {
                Text(
                    text = formatDayMonthYear(transaction.dateMillis),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = transaction.description.ifBlank { transaction.rawAmount },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = transaction.category.takeIf { it.isNotBlank() }
                        ?.let { localizedCategoryName(it) }
                        ?: stringResource(Res.string.smart_import_no_category),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.clickable(onClick = onToggleType)) {
                    NeoTag(
                        text = stringResource(
                            if (candidate.isExpense) Res.string.entry_type_expense else Res.string.entry_type_income
                        ),
                        backgroundColor = if (candidate.isExpense) BrutalPink else BrutalCyan,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** A neo-brutalist stand-in for a checkbox, matching the rest of the app's controls. */
@Composable
private fun SelectionBox(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .background(if (selected) BrutalCyan else LightBackground, RectangleShape)
            .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (selected) "✓" else " ",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
private fun MappingTag(mapping: FieldMapping) {
    NeoTag(
        text = stringResource(
            Res.string.smart_import_mapping_item,
            stringResource(fieldLabel(mapping.field)),
            mapping.columnIndex + 1,
            (mapping.confidence * 100).roundToInt(),
        ),
        backgroundColor = BrutalYellow,
        textColor = MaterialTheme.colorScheme.onSecondary,
    )
}

@Composable
private fun DoneContent(state: SmartImportUiState, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeoCard(backgroundColor = BrutalCyan) {
            Column {
                Text(
                    text = stringResource(Res.string.smart_import_done_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        Res.string.smart_import_done_summary,
                        state.importedCount,
                        state.duplicateCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
                if (state.newCategoryCount > 0) {
                    Text(
                        text = stringResource(
                            Res.string.smart_import_done_categories,
                            state.newCategoryCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        NeoButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            text = stringResource(Res.string.smart_import_btn_done),
        )
    }
}

private fun fieldLabel(field: TransactionField) = when (field) {
    TransactionField.DATE -> Res.string.smart_import_field_date
    TransactionField.AMOUNT -> Res.string.smart_import_field_amount
    TransactionField.CREDIT_AMOUNT -> Res.string.smart_import_field_credit
    TransactionField.DESCRIPTION -> Res.string.smart_import_field_description
    TransactionField.CATEGORY -> Res.string.smart_import_field_category
    TransactionField.TYPE -> Res.string.smart_import_field_type
}

@Composable
private fun sourceLabel(source: SmartImportSource) = stringResource(
    when (source) {
        SmartImportSource.IMAGE -> Res.string.smart_import_source_image
        SmartImportSource.CSV -> Res.string.smart_import_source_csv
        SmartImportSource.ODS -> Res.string.smart_import_source_ods
        SmartImportSource.PDF -> Res.string.smart_import_source_pdf
    }
)
