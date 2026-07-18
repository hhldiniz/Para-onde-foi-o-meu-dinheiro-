package com.hhldiniz.praondefoiomeudinheiro

import android.app.Application
import com.hhldiniz.praondefoiomeudinheiro.data.local.AppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository

class PraondefoiomeudinheiroApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val importRepository: ImportRepository by lazy { ImportRepository(database.importedEntryDao()) }
    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database.categoryDao()) }
}
