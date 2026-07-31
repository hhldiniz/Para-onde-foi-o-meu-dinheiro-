package com.hhldiniz.praondefoiomeudinheiro

import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.PatrimonyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.createKeyValueStore
import com.hhldiniz.praondefoiomeudinheiro.di.appModule
import com.hhldiniz.praondefoiomeudinheiro.di.platformModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Shared startup sequence, called once from each platform's entry point
 * (`PraondefoiomeudinheiroApp.onCreate` on Android, `MainViewController` on
 * iOS). [platformConfig] is where Android hands Koin its `Context`.
 */
object AppInitializer {

    private var initialized = false

    fun init(platformConfig: KoinAppDeclaration = {}) {
        if (initialized) return
        initialized = true

        CurrencyHolder.init(createKeyValueStore(CurrencyHolder.PREFS_NAME))
        PatrimonyHolder.init(createKeyValueStore(PatrimonyHolder.PREFS_NAME))

        startKoin {
            platformConfig()
            modules(appModule, platformModule)
        }
    }
}
