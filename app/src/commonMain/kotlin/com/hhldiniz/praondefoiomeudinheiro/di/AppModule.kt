package com.hhldiniz.praondefoiomeudinheiro.di

import com.hhldiniz.praondefoiomeudinheiro.data.chat.ChatModelManager
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.FileSpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.InvestmentRepository
import com.hhldiniz.praondefoiomeudinheiro.data.vision.SmartImportAnalyzer
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry.AddEntryViewModel
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.chat.ChatViewModel
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
    // One per process: the model's download and load outlive the chat screen.
    single { ChatModelManager(get()) }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { LandingViewModel(get()) }
    viewModel { AddEntryViewModel(get(), get()) }
    viewModel { IntroPatrimonyViewModel() }
    viewModel { IntroCategoriesViewModel(get()) }
    viewModel { SmartImportViewModel(get(), get(), get()) }
    viewModel { InvestmentsViewModel(get()) }
    viewModel { ChatViewModel(get(), get(), get()) }
}

/**
 * Bindings that can only be created with platform APIs; also supplies
 * [ImportedEntryDao]/[CategoryDao] and the assistant chat's
 * [com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntime].
 */
expect val platformModule: Module
