package com.hhldiniz.praondefoiomeudinheiro

import android.app.Application
import com.hhldiniz.praondefoiomeudinheiro.data.local.PdfBoxInitializer
import com.hhldiniz.praondefoiomeudinheiro.platform.AndroidAppContext
import org.koin.android.ext.koin.androidContext

class PraondefoiomeudinheiroApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Must come first: the shared initializer opens SharedPreferences.
        AndroidAppContext.init(this)
        PdfBoxInitializer.init(this)
        AppInitializer.init {
            androidContext(this@PraondefoiomeudinheiroApp)
        }
    }
}
