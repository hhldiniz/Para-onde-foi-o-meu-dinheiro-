package com.hhldiniz.praondefoiomeudinheiro.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.RoomCategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.RoomImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.CategoryRecord
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntryRecord
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.defaultCategories
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher

const val DATABASE_NAME = "praondefoiomeudinheiro.db"

@Database(entities = [ImportedEntryRecord::class, CategoryRecord::class], version = 3, exportSchema = false)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun importedEntryDao(): RoomImportedEntryDao
    abstract fun categoryDao(): RoomCategoryDao
}

/**
 * Room generates the `actual` implementations of this per target; the
 * `expect` declaration is what lets the `@Database` class itself live in
 * `roomMain`, shared by Android and iOS.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * Finishes wiring a platform-provided [RoomDatabase.Builder] and opens the
 * database. Schema changes recreate the database (see
 * `fallbackToDestructiveMigration`) since this is unsynced local data, and the
 * default categories are seeded on first creation.
 */
fun RoomDatabase.Builder<AppDatabase>.buildAppDatabase(): AppDatabase =
    fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(connection: SQLiteConnection) {
                super.onCreate(connection)
                val statement = connection.prepare("INSERT OR IGNORE INTO categories (name) VALUES (?)")
                try {
                    defaultCategories().forEach { category ->
                        statement.bindText(1, category.name)
                        statement.step()
                        statement.reset()
                    }
                } finally {
                    statement.close()
                }
            }
        })
        .build()

/**
 * Supplies the platform's [RoomDatabase.Builder]: Android needs a `Context`
 * to resolve the database path, iOS resolves it against the app's Documents
 * directory.
 */
expect class DatabaseBuilderFactory {
    fun create(): RoomDatabase.Builder<AppDatabase>
}
