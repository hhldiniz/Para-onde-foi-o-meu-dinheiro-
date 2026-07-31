package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseBuilderFactory(private val context: Context) {

    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(DATABASE_NAME)
        return Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath,
        )
    }
}
