package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.verticalScroll
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
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedReceipt
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.FieldMapping
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportSource
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.TransactionField
import com.hhldiniz.praondefoiomeudinheiro.platform.currencyFormatter
import com.hhldiniz.praondefoiomeudinheiro.platform.rememberImportSourcePicker
import com.hhldiniz.praondefoiomeudinheiro.platform.rememberReceiptPicker
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
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_btn_itemize
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_btn_receipt
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_btn_use_total
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
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_document
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_intro
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_items
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_items_mismatch
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_no_items
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_no_merchant
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_title
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_total
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_receipt_unlabelled_total
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_review_title
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_csv
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_image
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_ods
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_source_receipt
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_summary
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_title
import com.hhldiniz.praondefoiomeudinheiro.resources.smart_import_toggle_hint
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * The automatic import screen, with the two readings it offers side by side:
 * a **statement** (photo, screenshot or spreadsheet) whose columns the
 * classifier works out, and a **receipt** photographed and read by computer
 * vision into one purchase. Either way the user sees what was recognized and
 * confirms before anything is written.
 *
 * PDFs are not offered here — the direct import on the Home screen reads their
 * text exactly, and remains the right path for them and for files already in
 * the expected format.
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
    val receiptPicker = rememberReceiptPicker { file -> viewModel.onReceiptPicked(file) }

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
                    onPickReceipt = { receiptPicker.launch() },
                )

                SmartImportStage.ANALYZING -> AnalyzingContent(fileName = uiState.fileName)

                SmartImportStage.REVIEW, SmartImportStage.SAVING -> ReviewContent(
                    state = uiState,
                    saving = uiState.stage == SmartImportStage.SAVING,
                    onToggle = viewModel::onCandidateToggled,
                    onToggleType = viewModel::onCandidateTypeToggled,
                    onConfirm = viewModel::confirmImport,
                    onItemizedToggled = viewModel::onItemizedToggled,
                    onPickAnother = {
                        val wasReceipt = uiState.mode == SmartImportMode.RECEIPT
                        viewModel.reset()
                        if (wasReceipt) receiptPicker.launch() else picker.launch()
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
private fun IdleContent(error: String?, onPick: () -> Unit, onPickReceipt: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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

        Spacer(Modifier.height(20.dp))

        NeoCard(backgroundColor = BrutalYellow) {
            Text(
                text = stringResource(Res.string.smart_import_receipt_intro),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        NeoButton(
            onClick = onPickReceipt,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            text = stringResource(Res.string.smart_import_btn_receipt),
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
    onItemizedToggled: () -> Unit,
    onPickAnother: () -> Unit,
) {
    val currency by CurrencyHolder.selectedCurrency.collectAsStateWithLifecycle()
    val format = remember(currency) { currencyFormatter(currency) }

    Column(modifier = Modifier.fillMaxSize()) {
        val receipt = state.receipt
        if (state.mode == SmartImportMode.RECEIPT && receipt != null) {
            ReceiptSummaryCard(receipt = receipt, formatAmount = { format.format(it) })
        } else {
            StatementSummaryCard(state = state)
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

        if (state.mode == SmartImportMode.RECEIPT && state.canItemize) {
            NeoButton(
                onClick = onItemizedToggled,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalYellow,
                textColor = MaterialTheme.colorScheme.onSecondary,
                enabled = !saving,
                text = stringResource(
                    if (state.itemized) Res.string.smart_import_btn_use_total
                    else Res.string.smart_import_btn_itemize
                ),
            )

            Spacer(Modifier.height(8.dp))
        }

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

/**
 * What the classifier made of a statement: the file it read, how many rows it
 * scanned, and which column it took for which field.
 */
@Composable
private fun StatementSummaryCard(state: SmartImportUiState) {
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
}

/**
 * What the receipt reader made of the photo: who was paid, when, how much, and
 * how sure it is — plus the two things worth a warning, a total it had to
 * guess and items that do not add up to it.
 */
@Composable
private fun ReceiptSummaryCard(receipt: DetectedReceipt, formatAmount: (Double) -> String) {
    NeoCard {
        Column {
            Text(
                text = stringResource(Res.string.smart_import_receipt_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = receipt.merchant.ifBlank {
                    stringResource(Res.string.smart_import_receipt_no_merchant)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (receipt.documentId.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.smart_import_receipt_document, receipt.documentId),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.smart_import_receipt_total, formatAmount(receipt.total)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = if (receipt.items.isEmpty()) {
                    stringResource(Res.string.smart_import_receipt_no_items)
                } else {
                    stringResource(
                        Res.string.smart_import_receipt_items,
                        receipt.items.size,
                        (receipt.confidence * 100).roundToInt(),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                NeoTag(
                    text = stringResource(Res.string.smart_import_source_receipt),
                    backgroundColor = BrutalCyan,
                )
                NeoTag(
                    text = formatDayMonthYear(receipt.dateMillis),
                    backgroundColor = BrutalYellow,
                    textColor = MaterialTheme.colorScheme.onSecondary,
                )
                if (receipt.category.isNotBlank()) {
                    NeoTag(
                        text = localizedCategoryName(receipt.category),
                        backgroundColor = BrutalYellow,
                        textColor = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
            if (!receipt.totalWasLabelled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.smart_import_receipt_unlabelled_total),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (receipt.items.isNotEmpty() && !receipt.itemsMatchTotal) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        Res.string.smart_import_receipt_items_mismatch,
                        formatAmount(receipt.itemsTotal),
                        formatAmount(receipt.total),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
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
    }
)
