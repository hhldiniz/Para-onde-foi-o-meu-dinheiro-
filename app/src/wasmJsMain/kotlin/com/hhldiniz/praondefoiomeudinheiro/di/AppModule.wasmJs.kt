package com.hhldiniz.praondefoiomeudinheiro.di

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.InvestmentDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.web.WebCategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.web.WebImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.web.WebInvestmentDao
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<ImportedEntryDao> { WebImportedEntryDao() }
    single<CategoryDao> { WebCategoryDao() }
    single<InvestmentDao> { WebInvestmentDao() }
}
