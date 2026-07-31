package com.hhldiniz.praondefoiomeudinheiro.di

import com.hhldiniz.praondefoiomeudinheiro.data.local.AppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.local.DatabaseBuilderFactory
import com.hhldiniz.praondefoiomeudinheiro.data.local.buildAppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseBuilderFactory(androidContext()) }
    single { get<DatabaseBuilderFactory>().create().buildAppDatabase() }
    single<ImportedEntryDao> { get<AppDatabase>().importedEntryDao() }
    single<CategoryDao> { get<AppDatabase>().categoryDao() }
}
