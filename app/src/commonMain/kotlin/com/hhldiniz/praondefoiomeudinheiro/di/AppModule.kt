package com.hhldiniz.praondefoiomeudinheiro.di

import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.InvestmentRepository
import com.hhldiniz.praondefoiomeudinheiro.data.vision.SmartImportAnalyzer
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry.AddEntryViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro.IntroCategoriesViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.investments.InvestmentsViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro.IntroPatrimonyViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing.LandingViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport.SmartImportViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Shared graph. [ImportedEntryDao]/[CategoryDao] bindings come from
 * [platformModule] — Room-backed on Android/iOS, localStorage-backed on
 * wasmJs — since Room has no wasmJs target (see the `roomMain` source set in
 * `app/build.gradle.kts`).
 */
val appModule = module {
    single { ImportRepository(get()) }
    single { CategoryRepository(get()) }
    single { InvestmentRepository(get()) }
    single<SpreadsheetRepository> { FileSpreadsheetRepository() }
    single { SmartImportAnalyzer() }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { LandingViewModel(get()) }
    viewModel { AddEntryViewModel(get(), get()) }
    viewModel { IntroPatrimonyViewModel() }
    viewModel { IntroCategoriesViewModel(get()) }
    viewModel { SmartImportViewModel(get(), get(), get()) }
    viewModel { InvestmentsViewModel(get()) }
}

/** Bindings that can only be created with platform APIs; also supplies [ImportedEntryDao]/[CategoryDao]. */
expect val platformModule: Module
