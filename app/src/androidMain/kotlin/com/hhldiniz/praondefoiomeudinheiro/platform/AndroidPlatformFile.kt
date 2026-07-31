package com.hhldiniz.praondefoiomeudinheiro.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder
import com.hhldiniz.praondefoiomeudinheiro.domain.file.hasSpreadsheetExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A Storage Access Framework document exposed to the shared code as a [PlatformFile]. */
class AndroidPlatformFile(
    private val context: Context,
    val uri: Uri,
    displayName: String? = null,
) : PlatformFile {

    override val name: String = displayName ?: resolveDisplayName(context, uri)

    override val identifier: String = uri.toString()

    override val mimeType: String? get() = context.contentResolver.getType(uri)

    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Cannot open $uri")
    }
}

/** A document tree the user granted access to. */
class AndroidPlatformFolder(
    private val context: Context,
    val treeUri: Uri,
) : PlatformFolder {

    override val name: String = treeUri.lastPathSegment ?: ""

    override val identifier: String = treeUri.toString()

    override suspend fun listSpreadsheetFiles(): List<PlatformFile> = withContext(Dispatchers.IO) {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        tree.listFiles()
            .filter { it.name?.hasSpreadsheetExtension() == true }
            .map { AndroidPlatformFile(context, it.uri, it.name) }
    }
}

/** Resolves the display file name from a content URI using [OpenableColumns.DISPLAY_NAME]. */
private fun resolveDisplayName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    val fromCursor = cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) it.getString(nameIndex) else null
    }
    return fromCursor ?: uri.lastPathSegment ?: ""
}
