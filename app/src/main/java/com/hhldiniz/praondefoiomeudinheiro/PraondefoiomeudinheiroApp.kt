package com.hhldiniz.praondefoiomeudinheiro

import android.app.Application
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PraondefoiomeudinheiroApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CurrencyHolder.init(this)
        startKoin {
            androidContext(this@PraondefoiomeudinheiroApp)
            modules(appModule)
        }
    }
}
