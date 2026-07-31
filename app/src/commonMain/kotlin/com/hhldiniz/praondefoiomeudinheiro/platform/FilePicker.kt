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
