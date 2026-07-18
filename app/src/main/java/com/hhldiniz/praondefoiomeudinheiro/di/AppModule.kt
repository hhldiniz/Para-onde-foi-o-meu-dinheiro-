package com.hhldiniz.praondefoiomeudinheiro.di

import com.hhldiniz.praondefoiomeudinheiro.data.local.AppDatabase
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry.AddEntryViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing.LandingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getInstance(androidContext()) }

    single { get<AppDatabase>().importedEntryDao() }
    single { get<AppDatabase>().categoryDao() }

    single { ImportRepository(get()) }
    single { CategoryRepository(get()) }
    single<SpreadsheetRepository> { FileSpreadsheetRepository() }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { LandingViewModel(get()) }
    viewModel { AddEntryViewModel(get(), get()) }
}
