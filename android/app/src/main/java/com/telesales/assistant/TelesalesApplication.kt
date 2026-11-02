package com.telesales.assistant

import android.app.Application
import com.telesales.assistant.di.AppContainer
import com.telesales.assistant.di.DefaultAppContainer
import com.telesales.assistant.reminder.NotificationHelper

class TelesalesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        NotificationHelper(this).createChannel()
    }
}

