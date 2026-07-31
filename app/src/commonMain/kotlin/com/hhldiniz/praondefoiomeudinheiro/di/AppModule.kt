package com.hhldiniz.praondefoiomeudinheiro.di

import com.hhldiniz.praondefoiomeudinheiro.data.local.AppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.local.buildAppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.local.DatabaseBuilderFactory
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry.AddEntryViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro.IntroCategoriesViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro.IntroPatrimonyViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing.LandingViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Shared graph. The only platform-supplied binding is [DatabaseBuilderFactory],
 * which comes from [platformModule].
 */
val appModule = module {
    single { get<DatabaseBuilderFactory>().create().buildAppDatabase() }

    single { get<AppDatabase>().importedEntryDao() }
    single { get<AppDatabase>().categoryDao() }

    single { ImportRepository(get()) }
    single { CategoryRepository(get()) }
    single<SpreadsheetRepository> { FileSpreadsheetRepository() }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { LandingViewModel(get()) }
    viewModel { AddEntryViewModel(get(), get()) }
    viewModel { IntroPatrimonyViewModel() }
    viewModel { IntroCategoriesViewModel(get()) }
}

/** Bindings that can only be created with platform APIs (Android `Context`, iOS file URLs). */
expect val platformModule: Module
