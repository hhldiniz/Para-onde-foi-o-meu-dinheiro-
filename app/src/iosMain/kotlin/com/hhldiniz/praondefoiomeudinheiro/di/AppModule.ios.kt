package com.hhldiniz.praondefoiomeudinheiro.di

import com.hhldiniz.praondefoiomeudinheiro.data.local.AppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.local.DatabaseBuilderFactory
import com.hhldiniz.praondefoiomeudinheiro.data.local.buildAppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.InvestmentDao
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseBuilderFactory() }
    single { get<DatabaseBuilderFactory>().create().buildAppDatabase() }
    single<ImportedEntryDao> { get<AppDatabase>().importedEntryDao() }
    single<CategoryDao> { get<AppDatabase>().categoryDao() }
    single<InvestmentDao> { get<AppDatabase>().investmentDao() }
}
