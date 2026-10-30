package com.telesales.assistant

import android.app.Application
import com.telesales.assistant.di.AppContainer
import com.telesales.assistant.di.DefaultAppContainer

class TelesalesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

