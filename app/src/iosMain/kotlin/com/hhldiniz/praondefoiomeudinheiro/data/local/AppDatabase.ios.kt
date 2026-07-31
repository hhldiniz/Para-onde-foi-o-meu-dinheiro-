package com.hhldiniz.praondefoiomeudinheiro.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class DatabaseBuilderFactory {

    @OptIn(ExperimentalForeignApi::class)
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val documentsDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        val path = requireNotNull(documentsDirectory?.path) { "Documents directory unavailable" }
        return Room.databaseBuilder<AppDatabase>(name = "$path/$DATABASE_NAME")
    }
}
