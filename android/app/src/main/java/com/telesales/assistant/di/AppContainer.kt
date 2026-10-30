package com.telesales.assistant.di

import android.content.Context

/**
 * Interface cho Dependency Container theo mô hình Manual DI (SRS mục 2 & 9.1).
 * Các dependency cụ thể (SettingsRepository, CustomerRepository, Database, ApiService)
 * sẽ được cung cấp qua container này ở các task A1..A8.
 */
interface AppContainer

class DefaultAppContainer(private val context: Context) : AppContainer

