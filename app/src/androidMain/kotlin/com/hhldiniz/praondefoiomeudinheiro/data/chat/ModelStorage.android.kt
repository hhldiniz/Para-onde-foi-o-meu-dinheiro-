package com.hhldiniz.praondefoiomeudinheiro.data.chat

import android.os.StatFs
import com.hhldiniz.praondefoiomeudinheiro.platform.AndroidAppContext
import java.io.File

/**
 * `noBackupFilesDir`: app-private, removed with the app, and excluded from
 * Auto Backup by the platform itself — the model is a gigabyte that can always
 * be downloaded again, so it must never count against the user's backup.
 */
internal actual fun modelDirectoryPath(): String {
    val directory = File(AndroidAppContext.require().noBackupFilesDir, "models")
    directory.mkdirs()
    return directory.absolutePath
}

internal actual fun freeSpaceBytes(path: String): Long? =
    runCatching { StatFs(path).availableBytes }.getOrNull()

internal actual fun processorCount(): Int = Runtime.getRuntime().availableProcessors()
