package com.hhldiniz.praondefoiomeudinheiro.platform

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder

/** MIME filter for the document picker; the app sniffs the real format by extension. */
private val DOCUMENT_MIME_TYPES = arrayOf("text/*", "*/*")

@Composable
actual fun rememberSpreadsheetFilePicker(onPicked: (PlatformFile) -> Unit): PickerLauncher {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            takePersistableReadPermission(context, uri)
            currentOnPicked(AndroidPlatformFile(context, uri))
        }
    }

    return remember(launcher) { PickerLauncher { launcher.launch(DOCUMENT_MIME_TYPES) } }
}

@Composable
actual fun rememberSpreadsheetFolderPicker(onPicked: (PlatformFolder) -> Unit): PickerLauncher {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            takePersistableReadPermission(context, uri)
            currentOnPicked(AndroidPlatformFolder(context, uri))
        }
    }

    return remember(launcher) { PickerLauncher { launcher.launch(null) } }
}

/**
 * Keeps read access across process restarts. Not all providers grant it, so a
 * refusal is ignored — the URI still works for the current session.
 */
private fun takePersistableReadPermission(context: android.content.Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    } catch (_: SecurityException) {
    }
}
