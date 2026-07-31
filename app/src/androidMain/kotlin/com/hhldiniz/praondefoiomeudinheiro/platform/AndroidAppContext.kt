package com.hhldiniz.praondefoiomeudinheiro.platform

import android.content.Context

/**
 * Application context for the few Android actuals that need one but are
 * reached from common code without a parameter to carry it (currently only
 * SharedPreferences). Set once from `PraondefoiomeudinheiroApp.onCreate`,
 * before anything else runs.
 */
object AndroidAppContext {

    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun require(): Context = checkNotNull(applicationContext) {
        "AndroidAppContext.init() must be called from Application.onCreate()"
    }
}
