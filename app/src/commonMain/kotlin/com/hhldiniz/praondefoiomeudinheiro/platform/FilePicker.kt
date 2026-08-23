package com.hhldiniz.praondefoiomeudinheiro.platform

import androidx.compose.runtime.Composable
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder

/** Handle returned by the picker composables; call [launch] from a click handler. */
fun interface PickerLauncher {
    fun launch()
}

/**
 * Remembers a system file picker limited to spreadsheet documents. Android
 * uses the Storage Access Framework, iOS `UIDocumentPickerViewController`.
 */
@Composable
expect fun rememberSpreadsheetFilePicker(onPicked: (PlatformFile) -> Unit): PickerLauncher

/** Remembers a system folder picker; the callback receives the chosen directory. */
@Composable
expect fun rememberSpreadsheetFolderPicker(onPicked: (PlatformFolder) -> Unit): PickerLauncher

/**
 * Remembers a picker for the automatic importer, which accepts images (a photo
 * or screenshot of a statement) *as well as* the CSV/ODS formats — the
 * classifier behind it treats both the same way, so there is no reason to make
 * the user choose the right kind of file up front. PDFs are deliberately not
 * offered here: they keep their text as text and belong to the direct import,
 * which reads them exactly.
 */
@Composable
expect fun rememberImportSourcePicker(onPicked: (PlatformFile) -> Unit): PickerLauncher

/**
 * Remembers an image-only picker for the receipt reader — a photo of a "nota
 * fiscal" and nothing else, since the whole point of that path is reading a
 * printed receipt with computer vision.
 */
@Composable
expect fun rememberReceiptPicker(onPicked: (PlatformFile) -> Unit): PickerLauncher
