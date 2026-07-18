package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry

/** Room database for the application, holding imported spreadsheet entries. */
@Database(entities = [ImportedEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /** Returns the DAO for [ImportedEntry] CRUD operations. */
    abstract fun importedEntryDao(): ImportedEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton [AppDatabase], creating it on first access
         * with destructive migration fallback enabled.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "praondefoiomeudinheiro.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
