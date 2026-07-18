package com.hhldiniz.praondefoiomeudinheiro

import android.app.Application
import com.hhldiniz.praondefoiomeudinheiro.data.local.AppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository

/** Custom [Application] subclass that lazily initialises the database and import repository singletons. */
class PraondefoiomeudinheiroApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val importRepository: ImportRepository by lazy { ImportRepository(database.importedEntryDao()) }
}
