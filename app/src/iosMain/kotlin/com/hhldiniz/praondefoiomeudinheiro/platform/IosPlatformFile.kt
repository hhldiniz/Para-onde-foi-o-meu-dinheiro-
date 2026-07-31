package com.hhldiniz.praondefoiomeudinheiro.platform

import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder
import com.hhldiniz.praondefoiomeudinheiro.domain.file.hasSpreadsheetExtension
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL

/**
 * A document the user picked through `UIDocumentPickerViewController`. Files
 * outside the app's own container are security-scoped, so every read is
 * wrapped in start/stop access calls.
 */
class IosPlatformFile(private val url: NSURL) : PlatformFile {

    override val name: String = url.lastPathComponent ?: ""

    override val identifier: String = url.absoluteString ?: ""

    override suspend fun readBytes(): ByteArray = url.withSecurityScope {
        NSData.dataWithContentsOfURL(url)?.toByteArray() ?: ByteArray(0)
    }
}

/** A directory the user picked; enumerated non-recursively, like Android's document tree. */
class IosPlatformFolder(private val url: NSURL) : PlatformFolder {

    override val name: String = url.lastPathComponent ?: ""

    override val identifier: String = url.absoluteString ?: ""

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun listSpreadsheetFiles(): List<PlatformFile> = url.withSecurityScope {
        val contents = NSFileManager.defaultManager.contentsOfDirectoryAtURL(
            url = url,
            includingPropertiesForKeys = null,
            options = 0u,
            error = null,
        ).orEmpty()

        contents.filterIsInstance<NSURL>()
            .filter { it.lastPathComponent?.hasSpreadsheetExtension() == true }
            .map { IosPlatformFile(it) }
    }
}

/** Runs [block] with security-scoped access to this URL held open. */
private inline fun <T> NSURL.withSecurityScope(block: () -> T): T {
    val acquired = startAccessingSecurityScopedResource()
    try {
        return block()
    } finally {
        if (acquired) stopAccessingSecurityScopedResource()
    }
}
