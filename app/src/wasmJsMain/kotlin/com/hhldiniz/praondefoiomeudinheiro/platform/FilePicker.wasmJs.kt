package com.hhldiniz.praondefoiomeudinheiro.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder
import com.hhldiniz.praondefoiomeudinheiro.domain.file.SPREADSHEET_EXTENSIONS
import com.hhldiniz.praondefoiomeudinheiro.domain.file.hasSpreadsheetExtension
import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.JsAny
import kotlin.js.Promise
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileList

/**
 * Reads a `File`'s bytes via a single `arrayBuffer()`/base64 round trip
 * instead of per-byte JS interop, which would be far too slow for anything
 * but the tiniest file. `kotlinx-browser`'s `File` binding has no typed
 * `arrayBuffer()` member, so the call itself goes through `js(...)`.
 */
private fun jsArrayBuffer(file: File): Promise<JsAny> = js("file.arrayBuffer()")

private fun arrayBufferToBase64(buffer: JsAny): String = js(
    """
    (function() {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        const chunkSize = 0x8000;
        for (let i = 0; i < bytes.length; i += chunkSize) {
            binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize));
        }
        return btoa(binary);
    })()
    """
)

@OptIn(ExperimentalEncodingApi::class)
private suspend fun File.readAllBytes(): ByteArray {
    val buffer = jsArrayBuffer(this).await()
    return Base64.decode(arrayBufferToBase64(buffer))
}

private fun FileList.toList(): List<File> = (0 until length).mapNotNull { item(it) }

/** [PlatformFile] backed by a browser `File` picked through a hidden `<input type="file">`. */
private class WebPlatformFile(private val file: File) : PlatformFile {
    override val name: String get() = file.name
    override val identifier: String get() = file.name
    override val mimeType: String? get() = file.type.ifEmpty { null }
    override suspend fun readBytes(): ByteArray = file.readAllBytes()
}

/** [PlatformFolder] backed by the files picked through a `webkitdirectory` input. */
private class WebPlatformFolder(
    override val name: String,
    private val files: List<File>,
) : PlatformFolder {
    override val identifier: String = name
    override suspend fun listSpreadsheetFiles(): List<PlatformFile> =
        files.filter { it.name.hasSpreadsheetExtension() }.map { WebPlatformFile(it) }
}

private fun createFileInput(directory: Boolean): HTMLInputElement {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    if (directory) {
        // webkitdirectory has no typed Kotlin property; it is a de facto
        // standard across browsers, set via the raw attribute.
        input.setAttribute("webkitdirectory", "")
        input.setAttribute("directory", "")
    } else {
        input.accept = SPREADSHEET_EXTENSIONS.joinToString(",")
    }
    input.style.display = "none"
    document.body?.appendChild(input)
    return input
}

@Composable
actual fun rememberSpreadsheetFilePicker(onPicked: (PlatformFile) -> Unit): PickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    // The change listener is registered once at creation time (not per
    // launch) so repeated picks don't accumulate duplicate listeners.
    val input = remember {
        createFileInput(directory = false).also { element ->
            element.addEventListener("change", {
                element.files?.toList()?.firstOrNull()?.let { currentOnPicked(WebPlatformFile(it)) }
            })
        }
    }
    return remember(input) {
        PickerLauncher {
            input.value = ""
            input.click()
        }
    }
}

@Composable
actual fun rememberSpreadsheetFolderPicker(onPicked: (PlatformFolder) -> Unit): PickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val input = remember {
        createFileInput(directory = true).also { element ->
            element.addEventListener("change", {
                val files = element.files?.toList().orEmpty()
                currentOnPicked(WebPlatformFolder(name = "selected folder", files = files))
            })
        }
    }
    return remember(input) {
        PickerLauncher {
            input.value = ""
            input.click()
        }
    }
}
