package com.hhldiniz.praondefoiomeudinheiro.data.chat

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * Application Support, flagged as excluded from iCloud backup: the model is a
 * gigabyte that can always be downloaded again. (Caches would also stay out
 * of backups, but iOS empties it under storage pressure, which would mean a
 * surprise re-download.)
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun modelDirectoryPath(): String {
    val fileManager = NSFileManager.defaultManager
    val support: NSURL? = fileManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val directory = requireNotNull(support?.URLByAppendingPathComponent("models", isDirectory = true)) {
        "Application Support directory unavailable"
    }
    fileManager.createDirectoryAtURL(directory, withIntermediateDirectories = true, attributes = null, error = null)
    directory.setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
    return requireNotNull(directory.path) { "Application Support directory has no path" }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun freeSpaceBytes(path: String): Long? {
    val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, error = null) ?: return null
    return (attributes[NSFileSystemFreeSize] as? NSNumber)?.longLongValue
}

internal actual fun processorCount(): Int = NSProcessInfo.processInfo.activeProcessorCount.toInt()
